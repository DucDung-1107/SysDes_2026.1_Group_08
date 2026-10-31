package vn.medora.api.identity;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

final class ApiErrors {
    private ApiErrors() { }

    // Only call with constant error codes; no request content goes into this JSON.
    static void write(HttpServletResponse response, int status, String code)
        throws IOException {
        response.setStatus(status);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write("{\"error\":\"" + code + "\"}");
    }
}
