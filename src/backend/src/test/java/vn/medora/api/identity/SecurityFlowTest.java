package vn.medora.api.identity;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.junit.jupiter.web.SpringJUnitWebConfig;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import tools.jackson.databind.json.JsonMapper;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

// Real MVC + Security filters; mocked persistence. No Docker/database needed.
@SpringJUnitWebConfig(SecurityFlowTest.TestConfiguration.class)
class SecurityFlowTest {
    static final UUID ACTOR_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");
    static final UUID FACILITY_ID = UUID.fromString("00000000-0000-0000-0000-000000000020");
    static final String PASSWORD = "SyntheticPassword!2026";

    @Configuration
    @EnableWebMvc
    @Import({SecurityConfiguration.class, PasswordConfiguration.class,
        AccountUserDetailsService.class, PermissionGuard.class, AuthController.class,
        AccountAdminController.class, AccountAdminService.class, PasswordPolicy.class,
        ApiExceptionHandler.class})
    static class TestConfiguration {
        @Bean UserAccountRepository accounts() { return mock(UserAccountRepository.class); }
        @Bean UserRoleAssignmentRepository assignments() {
            return mock(UserRoleAssignmentRepository.class);
        }
        @Bean FacilityRepository facilities() { return mock(FacilityRepository.class); }
        @Bean RoleRepository roles() { return mock(RoleRepository.class); }
        @Bean IdentityAuditRepository audit() { return mock(IdentityAuditRepository.class); }
    }

    @Autowired WebApplicationContext context;
    @Autowired UserAccountRepository accounts;
    @Autowired UserRoleAssignmentRepository assignments;
    @Autowired FacilityRepository facilities;
    @Autowired RoleRepository roles;
    @Autowired IdentityAuditRepository audit;
    @Autowired PasswordEncoder encoder;
    MockMvc mvc;
    UserAccount actor;

    @BeforeEach
    void setUp() {
        reset(accounts, assignments, facilities, roles, audit);
        actor = new UserAccount("admin.demo", "Demo Administrator", encoder.encode(PASSWORD));
        ReflectionTestUtils.setField(actor, "id", ACTOR_ID);
        ReflectionTestUtils.setField(actor, "version", 0L);
        when(accounts.findByUsername("admin.demo")).thenReturn(Optional.of(actor));
        when(accounts.findById(ACTOR_ID)).thenReturn(Optional.of(actor));
        when(assignments.findFacilityIds(ACTOR_ID)).thenReturn(Set.of(FACILITY_ID));
        when(assignments.findPermissionCodes(ACTOR_ID, FACILITY_ID, AccountStatus.ACTIVE))
            .thenReturn(Set.of("account.read", "account.create", "account.update",
                "account.assign_role"));
        when(accounts.findInFacility(eq(FACILITY_ID), any())).thenReturn(Page.empty());
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    record Token(MockHttpSession session, String header, String value) { }

    Token token(MockHttpSession session) throws Exception {
        var request = get("/api/auth/csrf");
        if (session != null) { request.session(session); }
        MvcResult result = mvc.perform(request).andExpect(status().isOk()).andReturn();
        var json = JsonMapper.builder().build()
            .readTree(result.getResponse().getContentAsString());
        return new Token((MockHttpSession) result.getRequest().getSession(false),
            json.get("headerName").asText(), json.get("token").asText());
    }

    MockHttpSession login() throws Exception {
        Token csrf = token(null);
        mvc.perform(post("/api/auth/login").session(csrf.session())
            .header(csrf.header(), csrf.value())
            .param("username", "ADMIN.DEMO").param("password", PASSWORD))
            .andExpect(status().isNoContent());
        return csrf.session();
    }

    @Test
    void loginRotatesSessionIdAndNeverReturnsOrRetainsPassword() throws Exception {
        Token csrf = token(null);
        String oldSessionId = csrf.session().getId();
        mvc.perform(post("/api/auth/login").session(csrf.session())
            .with(request -> {
                request.setRequestedSessionId(csrf.session().getId());
                request.setRequestedSessionIdValid(true);
                return request;
            })
            .header(csrf.header(), csrf.value())
            .param("username", "admin.demo").param("password", PASSWORD))
            .andExpect(status().isNoContent());
        assertThat(csrf.session().getId()).isNotEqualTo(oldSessionId);
        var securityContext = (SecurityContext) csrf.session()
            .getAttribute("SPRING_SECURITY_CONTEXT");
        var principal = (AccountPrincipal) securityContext.getAuthentication().getPrincipal();
        assertThat(principal.getPassword()).isNull();
        mvc.perform(get("/api/auth/me").session(csrf.session()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.username").value("admin.demo"))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
            .andExpect(jsonPath("$.password").doesNotExist());
    }

    @Test
    void missingCsrfIsRejectedAndAnonymousMeIsUnauthorized() throws Exception {
        mvc.perform(post("/api/auth/login")
            .param("username", "admin.demo").param("password", PASSWORD))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void wrongUnknownLockedAndDisabledCredentialsHaveSamePublicError() throws Exception {
        rejectLogin("admin.demo", "WrongPassword!2026");
        rejectLogin("unknown.demo", PASSWORD);
        actor.updateProfile("Demo Administrator", AccountStatus.LOCKED);
        rejectLogin("admin.demo", PASSWORD);
        actor.updateProfile("Demo Administrator", AccountStatus.DISABLED);
        rejectLogin("admin.demo", PASSWORD);
    }

    void rejectLogin(String username, String password) throws Exception {
        Token csrf = token(null);
        mvc.perform(post("/api/auth/login").session(csrf.session())
            .header(csrf.header(), csrf.value())
            .param("username", username).param("password", password))
            .andExpect(status().isUnauthorized())
            .andExpect(content().json("{\"error\":\"INVALID_CREDENTIALS\"}"));
    }

    @Test
    void authVersionChangeInvalidatesOldSession() throws Exception {
        MockHttpSession session = login();
        actor.invalidateSessions();
        mvc.perform(get("/api/auth/me").session(session))
            .andExpect(status().isUnauthorized());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void logoutRequiresFreshCsrfAndInvalidatesSession() throws Exception {
        MockHttpSession session = login();
        Token csrf = token(session);
        mvc.perform(post("/api/auth/logout").session(session)
            .header(csrf.header(), csrf.value())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void otherFacilityIsDeniedBeforeReadingAccountData() throws Exception {
        MockHttpSession session = login();
        UUID otherFacility = UUID.randomUUID();
        mvc.perform(get("/api/admin/facilities/" + otherFacility + "/accounts")
            .session(session)).andExpect(status().isForbidden());
        verify(accounts, never()).findInFacility(eq(otherFacility), any());
        mvc.perform(get("/api/admin/facilities/" + FACILITY_ID + "/accounts")
            .session(session)).andExpect(status().isOk());
    }

    @Test
    void administratorCannotGrantThemselvesClinicalRole() throws Exception {
        MockHttpSession session = login();
        Token csrf = token(session);
        mvc.perform(put("/api/admin/facilities/" + FACILITY_ID
            + "/accounts/" + ACTOR_ID + "/roles").session(session)
            .header(csrf.header(), csrf.value()).contentType("application/json")
            .content("{\"roles\":[\"DOCTOR\"],\"version\":0}"))
            .andExpect(status().isForbidden());
        verifyNoInteractions(audit, roles);
        verify(assignments, never()).save(any());
    }

    @Test
    void staleVersionRejectsMutationAndAudit() throws Exception {
        MockHttpSession session = login();
        Token csrf = token(session);
        UUID targetId = UUID.randomUUID();
        UserAccount target = new UserAccount("staff.demo", "Demo Staff", encoder.encode(PASSWORD));
        ReflectionTestUtils.setField(target, "id", targetId);
        ReflectionTestUtils.setField(target, "version", 2L);
        when(assignments.existsByAccount_IdAndFacility_Id(targetId, FACILITY_ID)).thenReturn(true);
        when(accounts.findForUpdate(targetId)).thenReturn(Optional.of(target));
        mvc.perform(put("/api/admin/facilities/" + FACILITY_ID + "/accounts/" + targetId)
            .session(session).header(csrf.header(), csrf.value())
            .contentType("application/json")
            .content("{\"displayName\":\"Changed Name\",\"status\":\"DISABLED\",\"version\":1}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.error").value("STALE_VERSION"));
        assertThat(target.getDisplayName()).isEqualTo("Demo Staff");
        assertThat(target.getStatus()).isEqualTo(AccountStatus.ACTIVE);
        verifyNoInteractions(audit);
    }
}
