package vn.medora.api.common;

import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;
import static vn.medora.api.common.ApiSupport.*;

@Component
public class RequestDeduplication {
    private final Store store;
    private final JsonMapper json = JsonMapper.builder().findAndAddModules().build();
    public RequestDeduplication(Store store) { this.store = store; }
    public String hash(Object body) {
        try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
            .digest(json.writeValueAsBytes(body))); }
        catch (Exception exception) { throw new IllegalStateException(exception); }
    }
    public Optional<Map<String, Object>> previous(UUID facility, UUID actor, String kind, String key, Object body) {
        if (key == null || !key.matches("[A-Za-z0-9._-]{8,100}")) { throw bad("IDEMPOTENCY_KEY_REQUIRED"); }
        var previous = store.optional("select * from medora.request_result where facility_id=? and actor_id=? and kind=? and request_key=?",
            facility, actor, kind, key);
        if (previous.isEmpty()) { return Optional.empty(); }
        if (!hash(body).equals(previous.get().get("requestHash"))) { throw conflict("IDEMPOTENCY_KEY_REUSED"); }
        @SuppressWarnings("unchecked") Map<String, Object> result = json.readValue((String) previous.get().get("response"), Map.class);
        return Optional.of(result);
    }
    public Map<String, Object> save(UUID facility, UUID actor, String kind, String key, Object body, Map<String, Object> response) {
        store.update("insert into medora.request_result(facility_id,actor_id,kind,request_key,request_hash,response) values(?,?,?,?,?,?)",
            facility, actor, kind, key, hash(body), json.writeValueAsString(response));
        return response;
    }
}
