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
class Stage4IT extends IntegrationSupport {
    @Test void migrationsHibernateAndDatabaseGuardsAreReal() {
        assertThat(jdbc.queryForObject("select count(*) from public.flyway_schema_history where success", Integer.class)).isEqualTo(7);
        assertThat(jdbc.queryForObject("show server_version", String.class)).startsWith("17.");
        assertThat(jdbc.queryForObject("select count(*) from medora.app_role", Integer.class)).isEqualTo(11);
        UUID event = UUID.randomUUID();
        jdbc.update("insert into medora.audit_event(id,actor_id,facility_id,action,target_type,target_id) values(?,?,?,'TEST','USER',?)",
            event, admin, facility, doctor);
        assertThatThrownBy(() -> jdbc.update("delete from medora.audit_event where id=?", event)).hasMessageContaining("append-only");
    }
    @Test void realHttpServerUsesSessionCookiesCsrfAndRevocation() throws Exception {
        var cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        var client = HttpClient.newBuilder().cookieHandler(cookies).build();
        String base = "http://127.0.0.1:" + port;
        assertThat(client.send(HttpRequest.newBuilder(URI.create(base + "/health")).GET().build(),
            HttpResponse.BodyHandlers.ofString()).body()).isEqualTo("UP");
        assertThat(client.send(HttpRequest.newBuilder(URI.create(base + "/ready")).GET().build(),
            HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(200);
        var csrf = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/csrf")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(csrf.headers().firstValue("set-cookie").orElseThrow()).contains("HttpOnly", "SameSite=Lax");
        var token = json.readTree(csrf.body());
        var login = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/login"))
            .header(token.get("headerName").asText(), token.get("token").asText())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .POST(HttpRequest.BodyPublishers.ofString("username=rec." + suffix + "&password=" + URLEncoder.encode(PASSWORD, java.nio.charset.StandardCharsets.UTF_8)))
            .build(), HttpResponse.BodyHandlers.ofString());
        assertThat(login.statusCode()).isEqualTo(204);
        var me = client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/me")).GET().build(), HttpResponse.BodyHandlers.ofString());
        assertThat(me.statusCode()).isEqualTo(200); assertThat(me.body()).doesNotContain("password", "hash");
        jdbc.update("update medora.user_account set auth_version=auth_version+1 where id=?", reception);
        assertThat(client.send(HttpRequest.newBuilder(URI.create(base + "/api/auth/me")).GET().build(),
            HttpResponse.BodyHandlers.ofString()).statusCode()).isEqualTo(401);
    }
    @Test void patientsRequireDuplicateReviewAndRejectStaleUpdatesAndWrongScope() throws Exception {
        UUID patient = patient("Nguyễn Văn Test");
        var duplicate = patientInput("Nguyen Van Test");
        ok(request(receptionistSession, post(url("/patients")), duplicate), 409);
        var candidates = ok(request(receptionistSession, post(url("/patients/duplicate-check")), duplicate), 200);
        assertThat(candidates.size()).isEqualTo(1);
        duplicate.put("duplicateReviewed", true); duplicate.put("duplicateReviewReason", "Verified different synthetic person");
        ok(request(receptionistSession, post(url("/patients")), duplicate), 201);
        var changed = patientInput("Changed Test"); changed.put("version", 0);
        ok(request(receptionistSession, put(url("/patients/" + patient)), changed), 200);
        ok(request(receptionistSession, put(url("/patients/" + patient)), changed), 409);
        ok(request(adminSession, get(url("/patients/" + patient)), null), 403);
        ok(request(receptionistSession, get("/api/facilities/" + otherFacility + "/patients/" + patient), null), 403);
        ok(request(receptionistSession, get(url("/patients")).param("size", "101"), null), 400);
    }
    @Test void bookingRetriesBufferCancellationAndAbsenceAreAtomic() throws Exception {
        UUID p = patient("Appointment Test"); var r = resources();
        var input = booking(p, r.room(), r.service(), r.at());
        JsonNode booked = ok(request(receptionistSession, post(url("/appointments")).header("Idempotency-Key", "book-test-1"), input), 201);
        UUID appointment = id(booked);
        var retry = ok(request(receptionistSession, post(url("/appointments")).header("Idempotency-Key", "book-test-1"), input), 201);
        assertThat(id(retry)).isEqualTo(appointment);
        ok(request(receptionistSession, post(url("/appointments")).header("Idempotency-Key", "book-test-1"),
            booking(p, r.room(), r.service(), r.at().plusSeconds(3600))), 409);
        ok(request(receptionistSession, post(url("/appointments")).header("Idempotency-Key", "book-test-2"),
            booking(p, r.room(), r.service(), r.at().plusSeconds(1200))), 409);
        ok(request(adminSession, post(url("/staff-schedules")), Map.of("providerId", doctor, "kind", "ABSENCE",
            "startsAt", r.at().toString(), "endsAt", r.at().plusSeconds(3600).toString())), 409);
        assertThat(jdbc.queryForObject("select count(*) from medora.resource_booking where appointment_id=? and active", Integer.class, appointment)).isEqualTo(2);
        ok(request(receptionistSession, post(url("/appointments/" + appointment + "/transitions")),
            Map.of("action", "CANCEL", "version", 0, "reason", "Synthetic cancellation")), 200);
        assertThat(jdbc.queryForObject("select count(*) from medora.resource_booking where appointment_id=? and active", Integer.class, appointment)).isZero();
        ok(request(receptionistSession, post(url("/appointments")).header("Idempotency-Key", "book-test-3"), input), 201);
    }
    @Test void concurrentBookingHasOneWinnerAndNoOrphanBookings() throws Exception {
        UUID p = patient("Concurrency Test"); var r = resources();
        var input = booking(p, r.room(), r.service(), r.at());
        MockHttpSession secondSession = login("rec." + suffix);
        JsonNode token1 = json.readTree(mvc.perform(get("/api/auth/csrf").session(receptionistSession)).andReturn().getResponse().getContentAsString());
        JsonNode token2 = json.readTree(mvc.perform(get("/api/auth/csrf").session(secondSession)).andReturn().getResponse().getContentAsString());
        var gate = new CountDownLatch(1);
        try (var executor = Executors.newFixedThreadPool(2)) {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                final int index = i;
                results.add(executor.submit(() -> {
                    gate.await(); var token = index == 0 ? token1 : token2;
                    return mvc.perform(post(url("/appointments")).session(index == 0 ? receptionistSession : secondSession)
                        .header(token.get("headerName").asText(), token.get("token").asText())
                        .header("Idempotency-Key", "parallel-key-" + index).contentType("application/json")
                        .content(json.writeValueAsString(input))).andReturn().getResponse().getStatus();
                }));
            }
            gate.countDown();
            assertThat(List.of(results.get(0).get(30, TimeUnit.SECONDS), results.get(1).get(30, TimeUnit.SECONDS)))
                .containsExactlyInAnyOrder(201, 409);
        }
        assertThat(jdbc.queryForObject("select count(*) from medora.appointment where facility_id=?", Integer.class, facility)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select count(*) from medora.resource_booking b join medora.appointment a on a.id=b.appointment_id where a.facility_id=?", Integer.class, facility)).isEqualTo(2);
    }
    @Test void checkInRejectsDuplicatesAndQueueAndClinicalReadsRespectAssignment() throws Exception {
        UUID p = patient("Checkin Test"); UUID encounter = walkIn(p);
        ok(request(receptionistSession, post(url("/check-ins")), Map.of("patientId", p,
            "doctorId", doctor, "identityVerified", true)), 409);
        assertThat(ok(request(nurseSession, get(url("/queue")), null), 200).size()).isEqualTo(1);
        assertThat(ok(request(otherDoctorSession, get(url("/queue")), null), 200).size()).isZero();
        ok(request(otherDoctorSession, get(url("/encounters/" + encounter)), null), 403);
        ok(request(receptionistSession, get(url("/encounters/" + encounter)), null), 403);
        ok(request(adminSession, get(url("/encounters/" + encounter)), null), 403);
        ok(request(nurseSession, post(url("/encounters/" + encounter + "/start")), Map.of("version", 0)), 403);
        assertThat(jdbc.queryForObject("select count(*) from medora.encounter where facility_id=?", Integer.class, facility)).isEqualTo(1);
    }
    @Test void availabilityRescheduleConfirmAndBookedCheckInKeepStateHistory() throws Exception {
        UUID p = patient("Scheduled Checkin"); var r = resources();
        JsonNode available = ok(request(receptionistSession, get(url("/availability"))
            .param("providerId", doctor.toString()).param("roomId", r.room().toString())
            .param("serviceId", r.service().toString()).param("from", r.at().toString())
            .param("to", r.at().plusSeconds(3600).toString()), null), 200);
        assertThat(available.size()).isGreaterThan(0);
        UUID appointment = id(ok(request(receptionistSession, post(url("/appointments"))
            .header("Idempotency-Key", "scheduled-checkin"), booking(p, r.room(), r.service(), r.at())), 201));
        var changed = Map.of("providerId", doctor, "roomId", r.room(), "serviceId", r.service(),
            "startsAt", r.at().plusSeconds(3600).toString(), "version", 0, "reason", "Changed requested time");
        ok(request(receptionistSession, put(url("/appointments/" + appointment + "/schedule")), changed), 200);
        ok(request(receptionistSession, put(url("/appointments/" + appointment + "/schedule")), changed), 409);
        ok(request(receptionistSession, post(url("/appointments/" + appointment + "/transitions")), Map.of("action", "CONFIRM", "version", 1)), 200);
        // Arrange a visit on the clinic's current day, including test runs near midnight.
        Instant today = LocalDate.now(ZoneId.of("Asia/Ho_Chi_Minh")).atTime(12, 0).atZone(ZoneId.of("Asia/Ho_Chi_Minh")).toInstant();
        jdbc.update("update medora.appointment set starts_at=?,ends_at=?,reserved_until=? where id=?",
            java.sql.Timestamp.from(today), java.sql.Timestamp.from(today.plusSeconds(1200)), java.sql.Timestamp.from(today.plusSeconds(1800)), appointment);
        JsonNode encounter = ok(request(receptionistSession, post(url("/check-ins")), Map.of("patientId", p,
            "appointmentId", appointment, "doctorId", doctor, "nurseId", nurse, "identityVerified", true)), 201);
        assertThat(encounter.get("appointmentId").asText()).isEqualTo(appointment.toString());
        assertThat(jdbc.queryForObject("select status from medora.appointment where id=?", String.class, appointment)).isEqualTo("CHECKED_IN");
        assertThat(ok(request(receptionistSession, get(url("/appointments/" + appointment + "/events")), null), 200).size()).isEqualTo(4);
    }
    @Test void absenceAndNoShowRejectInvalidStatesAndReleaseBookings() throws Exception {
        UUID p = patient("Absent Provider"); var r = resources();
        ok(request(adminSession, post(url("/staff-schedules")), Map.of("providerId", doctor, "kind", "ABSENCE",
            "startsAt", r.at().toString(), "endsAt", r.at().plusSeconds(3600).toString())), 201);
        ok(request(receptionistSession, post(url("/appointments")).header("Idempotency-Key", "absence-booking"),
            booking(p, r.room(), r.service(), r.at())), 409);
        UUID appointment = id(ok(request(receptionistSession, post(url("/appointments")).header("Idempotency-Key", "noshow-booking"),
            booking(p, r.room(), r.service(), r.at().plusSeconds(7200))), 201));
        ok(request(receptionistSession, post(url("/appointments/" + appointment + "/transitions")), Map.of("action", "NO_SHOW", "version", 0)), 409);
        Instant past = Instant.now().minusSeconds(3600);
        jdbc.update("update medora.appointment set starts_at=?,ends_at=?,reserved_until=? where id=?",
            java.sql.Timestamp.from(past), java.sql.Timestamp.from(past.plusSeconds(1200)), java.sql.Timestamp.from(past.plusSeconds(1800)), appointment);
        ok(request(receptionistSession, post(url("/appointments/" + appointment + "/transitions")), Map.of("action", "NO_SHOW", "version", 0)), 200);
        assertThat(jdbc.queryForObject("select count(*) from medora.resource_booking where appointment_id=? and active", Integer.class, appointment)).isZero();
        ok(request(receptionistSession, post(url("/check-ins")), Map.of("patientId", p,
            "appointmentId", appointment, "doctorId", doctor, "identityVerified", true)), 409);
    }
    @Test void screeningCorrectionsKeepOriginalMeasurementAndHistory() throws Exception {
        UUID encounter = walkIn(patient("Vital Test"));
        var input = new LinkedHashMap<String, Object>(Map.of("temperatureC", 37, "pulseBpm", 80,
            "measuredAt", Instant.now().minusSeconds(1).toString(), "verified", true));
        UUID first = id(ok(request(nurseSession, post(url("/encounters/" + encounter + "/vitals")), input), 201));
        input.put("pulseBpm", 82); input.put("supersedesId", first); input.put("correctionReason", "Corrected transcription");
        ok(request(nurseSession, post(url("/encounters/" + encounter + "/vitals")), input), 201);
        ok(request(nurseSession, post(url("/encounters/" + encounter + "/vitals")), input), 409);
        assertThat(jdbc.queryForObject("select pulse_bpm from medora.vital_sign where id=?", Integer.class, first)).isEqualTo(80);
        assertThatThrownBy(() -> jdbc.update("delete from medora.vital_sign where id=?", first)).hasMessageContaining("append-only");
        UUID history = id(ok(request(nurseSession, post(url("/encounters/" + encounter + "/history")),
            Map.of("kind", "ALLERGY", "content", "Synthetic unverified allergy history")), 201));
        ok(request(nurseSession, post(url("/encounters/" + encounter + "/history")),
            Map.of("kind", "HISTORY", "content", "Wrong predecessor type", "supersedesId", history, "correctionReason", "Test")), 400);
    }
    @Test void signedNoteAmendmentsAndClosurePreserveOriginalAndRejectStaleVersions() throws Exception {
        UUID encounter = walkIn(patient("Clinical Test")); start(encounter);
        String base = "/encounters/" + encounter;
        ok(request(doctorSession, post(url(base + "/submit-for-closure")), Map.of("version", 1)), 409);
        var content = new LinkedHashMap<String, Object>(Map.of("subjective", "Synthetic symptoms", "objective", "",
            "assessment", "Teaching example", "plan", "Synthetic plan"));
        UUID note = id(ok(request(doctorSession, post(url(base + "/notes")), content), 201));
        ok(request(doctorSession, post(url(base + "/notes/" + note + "/sign")), Map.of("version", 0)), 409);
        ok(request(doctorSession, post(url(base + "/diagnoses")), Map.of("code", "DEMO-001", "symptoms", "Synthetic symptoms")), 201);
        content.put("objective", "Synthetic examination"); content.put("version", 0);
        ok(request(doctorSession, put(url(base + "/notes/" + note)), content), 200);
        ok(request(doctorSession, put(url(base + "/notes/" + note)), content), 409);
        JsonNode signed = ok(request(doctorSession, post(url(base + "/notes/" + note + "/sign")), Map.of("version", 1)), 200);
        assertThat(signed.get("status").asText()).isEqualTo("SIGNED");
        content.put("version", 2);
        ok(request(doctorSession, put(url(base + "/notes/" + note)), content), 409);
        assertThatThrownBy(() -> jdbc.update("update medora.clinical_note set plan='tampered' where id=?", note)).hasMessageContaining("immutable");
        UUID amendment = id(ok(request(doctorSession, post(url(base + "/notes/" + note + "/amendments")),
            Map.of("version", 2, "reason", "Synthetic correction")), 201));
        content.put("version", 0); content.put("plan", "Corrected synthetic plan");
        ok(request(doctorSession, put(url(base + "/notes/" + amendment)), content), 200);
        ok(request(doctorSession, post(url(base + "/notes/" + amendment + "/sign")), Map.of("version", 1)), 200);
        assertThat(jdbc.queryForObject("select plan from medora.clinical_note where id=?", String.class, note)).isEqualTo("Synthetic plan");
        assertThat(jdbc.queryForObject("select predecessor_id from medora.clinical_note where id=?", UUID.class, amendment)).isEqualTo(note);
        ok(request(nurseSession, post(url(base + "/vitals")), Map.of("pulseBpm", 80,
            "measuredAt", Instant.now().minusSeconds(1).toString(), "verified", true)), 201);
        ok(request(doctorSession, post(url(base + "/submit-for-closure")), Map.of("version", 1)), 200);
        JsonNode closed = ok(request(doctorSession, post(url(base + "/complete")), Map.of("version", 2,
            "summary", "Synthetic summary", "instructions", "Synthetic instructions", "followUp", "No follow-up in this synthetic example")), 200);
        assertThat(closed.get("status").asText()).isEqualTo("COMPLETED");
        assertThatThrownBy(() -> jdbc.update("update medora.encounter set summary='tampered' where id=?", encounter)).hasMessageContaining("immutable");
        ok(request(doctorSession, post(url(base + "/notes/" + amendment + "/amendments")),
            Map.of("version", 2, "reason", "Post-close change")), 409);
        assertThat(jdbc.queryForObject("select count(*) from medora.audit_event where facility_id=? and target_id=?", Integer.class, facility, encounter)).isGreaterThan(8);
    }
}
