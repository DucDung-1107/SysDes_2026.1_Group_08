package vn.medora.api;

import java.util.Map;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import vn.medora.api.identity.UserRoleAssignment;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;

class AccountAdministrationIT extends IntegrationSupport {
    String adminUrl(String path) { return "/api/admin/facilities/" + facility + path; }

    @Test void accountApiReturnsEntitiesAndScopedPageWithoutCredentialFields() throws Exception {
        var created = ok(request(adminSession, post(adminUrl("/accounts")), Map.of(
            "username", "created." + suffix, "displayName", "API created nurse",
            "password", PASSWORD, "roles", List.of("NURSE"))), 201);
        UUID target = id(created);
        assertThat(created.has("password")).isFalse();
        assertThat(created.has("passwordHash")).isFalse();
        var page = ok(request(adminSession, get(adminUrl("/accounts")).param("size", "100"), null), 200);
        assertThat(page.get("totalElements").asInt()).isGreaterThan(1);
        assertThat(page.get("items").toString()).contains(target.toString());
        assertThat(ok(request(adminSession, get(adminUrl("/accounts/" + target)), null), 200)
            .get("username").asText()).isEqualTo("created." + suffix);
        assertThat(ok(request(adminSession, get(adminUrl("/roles")), null), 200).isArray()).isTrue();
        ok(request(adminSession, get(adminUrl("/accounts")).param("size", "101"), null), 400);
        ok(request(nurseSession, get(adminUrl("/accounts")), null), 403);
        ok(request(adminSession, get("/api/admin/facilities/" + otherFacility + "/accounts/" + target), null), 403);
        assertThat(ok(request(adminSession, get("/api/auth/me"), null), 200).get("id").asText()).isEqualTo(admin.toString());
        assertThat(ok(request(adminSession, get("/api/auth/permissions").param("facilityId", facility.toString()), null), 200).isArray()).isTrue();
        for (String health : List.of("/health", "/ready")) {
            var response = mvc.perform(get(health)).andReturn().getResponse();
            assertThat(response.getStatus()).isEqualTo(200);
            assertThat(response.getContentAsString()).isEqualTo("UP");
        }
        ok(request(adminSession, post("/api/auth/logout"), null), 204);
        assertThat(adminSession.isInvalid()).isTrue();
        ok(request(null, get("/api/auth/me"), null), 401);
    }

    @Test void updateRolesAndPasswordPersistAndRevokeExistingSessions() throws Exception {
        UUID target = id(ok(request(adminSession, post(adminUrl("/accounts")), Map.of(
            "username", "lifecycle." + suffix, "displayName", "Before update",
            "password", PASSWORD, "roles", List.of("DOCTOR"))), 201));
        var oldSession = login("lifecycle." + suffix);
        var updated = ok(request(adminSession, put(adminUrl("/accounts/" + target)),
            Map.of("displayName", "After update", "status", "ACTIVE", "version", 0)), 200);
        assertThat(updated.get("displayName").asText()).isEqualTo("After update");
        ok(request(adminSession, put(adminUrl("/accounts/" + target)),
            Map.of("displayName", "Stale overwrite", "status", "ACTIVE", "version", 0)), 409);
        var rolesChanged = ok(request(adminSession, put(adminUrl("/accounts/" + target + "/roles")),
            Map.of("roles", List.of("NURSE"), "version", updated.get("version").asLong())), 200);
        assertThat(rolesChanged.get("roles").toString()).contains("NURSE").doesNotContain("DOCTOR");
        assertThat(mvc.perform(get("/api/auth/me").session(oldSession)).andReturn().getResponse().getStatus()).isEqualTo(401);
        var refreshed = login("lifecycle." + suffix);
        String replacement = "ReplacementPassword!2026";
        ok(request(adminSession, put(adminUrl("/accounts/" + target + "/password")),
            Map.of("password", replacement, "version", rolesChanged.get("version").asLong())), 200);
        assertThat(mvc.perform(get("/api/auth/me").session(refreshed)).andReturn().getResponse().getStatus()).isEqualTo(401);
        var csrf = mvc.perform(get("/api/auth/csrf")).andReturn();
        var token = json.readTree(csrf.getResponse().getContentAsString());
        var session = (org.springframework.mock.web.MockHttpSession) csrf.getRequest().getSession();
        var response = mvc.perform(post("/api/auth/login").session(session)
            .header(token.get("headerName").asText(), token.get("token").asText())
            .param("username", "lifecycle." + suffix).param("password", replacement)).andReturn().getResponse();
        assertThat(response.getStatus()).isEqualTo(204);
        assertThat(ok(request(adminSession, get(adminUrl("/accounts/" + target)), null), 200)
            .get("displayName").asText()).isEqualTo("After update");
    }

    @Test void globalChangesRequireEveryAccountFacilityAndRejectSelfAdministration() throws Exception {
        var target = accounts.findById(nurse).orElseThrow();
        assignments.saveAndFlush(new UserRoleAssignment(target, roles.findById("NURSE").orElseThrow(),
            facilities.findById(otherFacility).orElseThrow()));
        long version = target.getVersion();
        ok(request(adminSession, put(adminUrl("/accounts/" + nurse)),
            Map.of("displayName", "Forbidden global change", "status", "LOCKED", "version", version)), 403);
        ok(request(adminSession, put(adminUrl("/accounts/" + nurse + "/password")),
            Map.of("password", "ReplacementPassword!2026", "version", version)), 403);
        ok(request(adminSession, post(adminUrl("/accounts/" + nurse + "/revoke-sessions")), Map.of("version", version)), 403);
        assertThat(accounts.findById(nurse).orElseThrow().getDisplayName()).isEqualTo(target.getDisplayName());
        assertThat(ok(request(nurseSession, get("/api/auth/me"), null), 200).get("id").asText()).isEqualTo(nurse.toString());
        ok(request(adminSession, put(adminUrl("/accounts/" + admin + "/roles")),
            Map.of("roles", List.of("NURSE"), "version", 0)), 403);
    }
}
