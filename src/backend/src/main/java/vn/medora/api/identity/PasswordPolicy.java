package vn.medora.api.identity;

import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public final class PasswordPolicy {
    private final PasswordEncoder encoder;
    public PasswordPolicy(PasswordEncoder encoder) { this.encoder = encoder; }
    public String encode(String password) {
        try { validate(password); }
        catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "PASSWORD_MUST_HAVE_12_CHARACTERS_AND_AT_MOST_72_UTF8_BYTES");
        }
        return encoder.encode(password);
    }
    public static void validate(String password) {
        if (password == null) {
            throw new IllegalArgumentException("Password cannot be null");
        }

        int unicodepoint = password.codePointCount(0, password.length());
        int byteutf8 = password.getBytes(StandardCharsets.UTF_8).length;
        if (unicodepoint < 12 || byteutf8 > 72) {
            throw new IllegalArgumentException("Password must have at least 12 characters and at most 72 UTF-8 bytes");
        }
    }
}
