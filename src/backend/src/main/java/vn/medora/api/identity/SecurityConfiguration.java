package vn.medora.api.identity;

import jakarta.servlet.DispatcherType;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfiguration {
    @Bean
    public DaoAuthenticationProvider accountAuthenticationProvider(
        AccountUserDetailsService users, PasswordEncoder encoder) {
        var provider = new DaoAuthenticationProvider(users);
        provider.setPasswordEncoder(encoder);
        provider.setHideUserNotFoundExceptions(true);
        return provider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
        DaoAuthenticationProvider provider, UserAccountRepository accounts)
        throws Exception {
        http.authenticationProvider(provider)
            .authorizeHttpRequests(auth -> auth
                .dispatcherTypeMatchers(DispatcherType.ERROR).permitAll()
                .requestMatchers(HttpMethod.GET, "/health", "/ready", "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/auth/me",
                    "/api/auth/permissions").authenticated()
                .requestMatchers("/api/admin/**", "/api/facilities/**").authenticated()
                .anyRequest().denyAll())
            // Keep Spring Security's default session-based CSRF protection.
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, exception) ->
                    ApiErrors.write(response, 401, "UNAUTHENTICATED"))
                .accessDeniedHandler((request, response, exception) ->
                    ApiErrors.write(response, 403, "FORBIDDEN")))
            .requestCache(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .sessionManagement(session -> session
                .sessionFixation(fixation -> fixation.changeSessionId()))
            .formLogin(login -> login
                .loginPage("/api/auth/login")
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) ->
                    response.setStatus(204))
                .failureHandler((request, response, exception) ->
                    ApiErrors.write(response, 401, "INVALID_CREDENTIALS")))
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .invalidateHttpSession(true)
                .clearAuthentication(true)
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, authentication) ->
                    response.setStatus(204))
                .permitAll())
            .addFilterAfter(new SessionValidityFilter(accounts),
                SecurityContextHolderFilter.class)
            .addFilterBefore(new CredentialsLengthFilter(),
                UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
