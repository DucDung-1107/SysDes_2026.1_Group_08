package vn.medora.api.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.text.Normalizer;
import java.util.Locale;
import java.util.Objects;

public final class ApiSupport {
    private ApiSupport() { }
    public static ResponseStatusException bad(String code) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, code);
    }
    public static ResponseStatusException conflict(String code) {
        return new ResponseStatusException(HttpStatus.CONFLICT, code);
    }
    public static ResponseStatusException missing() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
    }
    public static void version(ScopedEntity entity, Long expected) {
        if (!Objects.equals(entity.getVersion(), expected)) { throw conflict("STALE_VERSION"); }
    }
    public static PageRequest page(int page, int size, String sort) {
        if (page < 0 || size < 1 || size > 100 || (long) page * size > 1_000_000) {
            throw bad("INVALID_PAGINATION");
        }
        return PageRequest.of(page, size, Sort.by(sort).and(Sort.by("id")));
    }
    public static String normalized(String value) {
        return Normalizer.normalize(value.strip(), Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "").toLowerCase(Locale.ROOT)
            .replace('đ', 'd').replaceAll("\\s+", " ");
    }
    public static String optional(String value) {
        return value == null || value.isBlank() ? null : value.strip();
    }
    public static String like(String value) {
        return "%" + normalized(value).replace("!", "!!").replace("%", "!%")
            .replace("_", "!_") + "%";
    }
}
