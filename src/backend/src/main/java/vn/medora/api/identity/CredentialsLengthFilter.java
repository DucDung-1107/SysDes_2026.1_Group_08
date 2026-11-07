package vn.medora.api.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class CredentialsLengthFilter extends OncePerRequestFilter {
    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        if ("POST".equals(request.getMethod())
            && "/api/auth/login".equals(request.getServletPath())) {
            String username = request.getParameter("username");
            String password = request.getParameter("password");
            if (username == null || username.length() > 64
                || password == null || password.isEmpty()
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
                ApiErrors.write(response, 401, "INVALID_CREDENTIALS");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
