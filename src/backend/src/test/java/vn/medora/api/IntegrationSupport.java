package vn.medora.api;

import io.zonky.test.db.postgres.embedded.EmbeddedPostgres;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.mock.web.MockHttpSession;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;
import vn.medora.api.identity.*;
import java.nio.file.*;
import java.net.*;
import java.net.http.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

// Full application, actual PostgreSQL 17 + Flyway + Hibernate + MVC/Security filters.
// This isolated database never uses a developer's datasource or clinic records.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {"server.address=127.0.0.1", "debug=false", "logging.level.org.springframework=INFO"})
abstract class IntegrationSupport {
    static final EmbeddedPostgres POSTGRES;
    static final String JDBC_URL;
    static {
        try {
            Path root = Files.createDirectories(Path.of("target", "postgres-it", UUID.randomUUID().toString()).toAbsolutePath());
            POSTGRES = EmbeddedPostgres.builder().setOverrideWorkingDirectory(root.resolve("bin").toFile())
                .setDataDirectory(root.resolve("data")).setServerConfig("listen_addresses", "127.0.0.1").start();
            try (var connection = POSTGRES.getPostgresDatabase().getConnection()) {
                JDBC_URL = connection.getMetaData().getURL();
            }
            Runtime.getRuntime().addShutdownHook(new Thread(() -> { try { POSTGRES.close(); } catch (Exception ignored) { } }));
        } catch (Exception exception) { throw new ExceptionInInitializerError(exception); }
    }
    @DynamicPropertySource static void database(DynamicPropertyRegistry properties) {
        properties.add("spring.datasource.url", () -> JDBC_URL);
        properties.add("spring.datasource.username", () -> "postgres");
        properties.add("spring.datasource.password", () -> "");
        properties.add("medora.storage.path", () -> Path.of("target", "test-attachments").toAbsolutePath().toString());
        properties.add("medora.notifications.poll-ms", () -> "600000");
    }
    @Autowired WebApplicationContext context;
    @Autowired JdbcTemplate jdbc;
    @Autowired UserAccountRepository accounts;
    @Autowired FacilityRepository facilities;
    @Autowired RoleRepository roles;
    @Autowired UserRoleAssignmentRepository assignments;
    @Autowired PasswordEncoder encoder;
    @org.springframework.beans.factory.annotation.Value("${local.server.port}") int port;
    final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    MockMvc mvc;
    UUID facility, otherFacility, doctor, nurse, reception, admin, otherDoctor;
    String suffix;
    MockHttpSession receptionistSession, doctorSession, nurseSession, adminSession, otherDoctorSession;
    static final String PASSWORD = "SyntheticPassword!2026";
    record Result(int status, JsonNode body) { }
    @BeforeEach void setup() throws Exception {
        ApiCoverageRecorder.register(context);
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity())
            .alwaysDo(ApiCoverageRecorder::record).build();
        suffix = UUID.randomUUID().toString().substring(0, 8);
        facility = facilities.saveAndFlush(new Facility("TEST-" + suffix, "Synthetic Test Clinic")).getId();
        otherFacility = facilities.saveAndFlush(new Facility("OTHER-" + suffix, "Other Clinic")).getId();
        reception = account("rec", "RECEPTIONIST"); doctor = account("doc", "DOCTOR");
        nurse = account("nurse", "NURSE"); admin = account("admin", "SYSTEM_ADMINISTRATOR");
        otherDoctor = account("otherdoc", "DOCTOR");
        receptionistSession = login("rec." + suffix); doctorSession = login("doc." + suffix);
        nurseSession = login("nurse." + suffix); adminSession = login("admin." + suffix);
        otherDoctorSession = login("otherdoc." + suffix);
    }
    @AfterAll static void routeEvidence() throws java.io.IOException { ApiCoverageRecorder.write(); }
    UUID account(String name, String role) {
        var account = accounts.saveAndFlush(new UserAccount(name + "." + suffix, "Synthetic " + name, encoder.encode(PASSWORD)));
        assignments.saveAndFlush(new UserRoleAssignment(account, roles.findById(role).orElseThrow(), facilities.findById(facility).orElseThrow()));
        return account.getId();
    }
    MockHttpSession login(String username) throws Exception {
        var csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        var token = json.readTree(csrf.getResponse().getContentAsString());
        var session = (MockHttpSession) csrf.getRequest().getSession();
        var result = mvc.perform(post("/api/auth/login").session(session)
            .header(token.get("headerName").asText(), token.get("token").asText())
            .param("username", username).param("password", PASSWORD)).andReturn();
        assertThat(result.getResponse().getStatus()).isEqualTo(204);
        return session;
    }
    Result request(MockHttpSession session, org.springframework.test.web.servlet.request.AbstractMockHttpServletRequestBuilder<?> request, Object body) throws Exception {
        if (session != null) {
            var csrf = mvc.perform(get("/api/auth/csrf").session(session)).andReturn();
            var token = json.readTree(csrf.getResponse().getContentAsString());
            request.session(session).header(token.get("headerName").asText(), token.get("token").asText());
        }
        if (body != null) { request.contentType("application/json").content(json.writeValueAsString(body)); }
        var response = mvc.perform(request).andReturn().getResponse();
        String content = response.getContentAsString();
        return new Result(response.getStatus(), content.isEmpty() ? json.nullNode() : json.readTree(content));
    }
    String url(String path) { return "/api/facilities/" + facility + path; }
    JsonNode ok(Result result, int expected) {
        assertThat(result.status()).describedAs(result.body().toString()).isEqualTo(expected); return result.body();
    }
    UUID id(JsonNode node) { return UUID.fromString(node.get("id").asText()); }
    Map<String, Object> patientInput(String name) {
        return new LinkedHashMap<>(Map.of("fullName", name, "dateOfBirth", "2000-01-02", "sex", "UNKNOWN"));
    }
    UUID patient(String name) throws Exception {
        return id(ok(request(receptionistSession, post(url("/patients")), patientInput(name)), 201));
    }
    UUID walkIn(UUID patient) throws Exception {
        return id(ok(request(receptionistSession, post(url("/check-ins")), Map.of("patientId", patient,
            "doctorId", doctor, "nurseId", nurse, "identityVerified", true)), 201));
    }
    JsonNode start(UUID encounter) throws Exception {
        return ok(request(doctorSession, post(url("/encounters/" + encounter + "/start")), Map.of("version", 0)), 200);
    }
    Map<String, Object> booking(UUID patient, UUID room, UUID service, Instant at) {
        return Map.of("patientId", patient, "providerId", doctor, "roomId", room, "serviceId", service, "startsAt", at.toString());
    }
    record Resources(UUID room, UUID service, Instant at) { }
    Resources resources() throws Exception {
        UUID room = id(ok(request(adminSession, post(url("/rooms")), Map.of("code", "R1", "name", "Demo Room",
            "roomType", "CONSULTATION", "equipment", "", "enabled", true)), 201));
        UUID service = id(ok(request(adminSession, post(url("/services")), Map.of("code", "S1", "name", "Demo Consultation",
            "durationMinutes", 20, "bufferMinutes", 10, "enabled", true)), 201));
        ok(request(adminSession, post(url("/provider-services")), Map.of("providerId", doctor, "serviceId", service)), 204);
        Instant at = Instant.now().plusSeconds(3600).truncatedTo(java.time.temporal.ChronoUnit.MINUTES);
        ok(request(adminSession, post(url("/staff-schedules")), Map.of("providerId", doctor, "kind", "WORK",
            "startsAt", at.minusSeconds(3600).toString(), "endsAt", at.plusSeconds(28800).toString())), 201);
        return new Resources(room, service, at);
    }

}
