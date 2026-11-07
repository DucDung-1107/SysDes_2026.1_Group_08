package vn.medora.api.identity;

import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Locale;

@Service
public class AccountUserDetailsService implements UserDetailsService {
    private final UserAccountRepository accounts;

    public AccountUserDetailsService(UserAccountRepository accounts) {
        this.accounts = accounts;
    }

    @Override
    @Transactional(readOnly = true)
    public AccountPrincipal loadUserByUsername(String username) {
        String normalized = username.strip().toLowerCase(Locale.ROOT);
        UserAccount account = accounts.findByUsername(normalized)
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
        return new AccountPrincipal(account);
    }
}
