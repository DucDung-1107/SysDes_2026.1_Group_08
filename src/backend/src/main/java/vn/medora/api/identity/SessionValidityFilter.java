package vn.medora.api.identity;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import java.io.IOException;

// Constructed by SecurityConfiguration only; not registered as a servlet bean.
public class SessionValidityFilter extends OncePerRequestFilter {
    private final UserAccountRepository accounts;

    public SessionValidityFilter(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
        HttpServletResponse response, FilterChain chain)
        throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext()
            .getAuthentication();
        if (authentication != null
            && authentication.getPrincipal() instanceof AccountPrincipal principal) {
            UserAccount account = accounts.findById(principal.getAccountId())
                .orElse(null);
            if (account == null || account.getStatus() != AccountStatus.ACTIVE
                || account.getAuthVersion() != principal.getAuthVersion()) {
                var session = request.getSession(false);
                if (session != null) { session.invalidate(); }
                SecurityContextHolder.clearContext();
                ApiErrors.write(response, 401, "UNAUTHENTICATED");
                return;
            }
        }
        chain.doFilter(request, response);
    }
}
