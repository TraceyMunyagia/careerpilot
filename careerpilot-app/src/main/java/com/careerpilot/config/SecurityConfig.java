package com.careerpilot.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter;
import java.time.Duration;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /** Pages anyone can see without logging in. Everything else requires a login. */
    private static final String[] PUBLIC_PATHS = {
            "/",
            "/register",
            "/error",
            "/css/**",
            "/js/**",
            "/images/**",
            "/favicon.ico",
            "/actuator/health"
    };

      @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            @Value("${careerpilot.security.remember-me-key}") String rememberMeKey) throws Exception {
        http
                // Rules are checked top to bottom; the first match wins
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PUBLIC_PATHS).permitAll()
                        .anyRequest().authenticated())
                .formLogin(form -> form
                        .loginPage("/login")
                        .usernameParameter("email")
                        .defaultSuccessUrl("/dashboard")
                        .failureUrl("/login?error")
                        .permitAll())
                // A signed cookie that logs you back in after the browser closes
                .rememberMe(remember -> remember
                        .key(rememberMeKey)
                        .rememberMeParameter("remember-me")
                        .tokenValiditySeconds((int) Duration.ofDays(14).toSeconds()))
                              .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .deleteCookies("JSESSIONID", "remember-me")
                        .permitAll())
                .headers(headers -> headers
                        .contentSecurityPolicy(csp -> csp.policyDirectives(CONTENT_SECURITY_POLICY))
                        .referrerPolicy(referrer -> referrer
                                .policy(ReferrerPolicyHeaderWriter.ReferrerPolicy.STRICT_ORIGIN_WHEN_CROSS_ORIGIN)));
        return http.build();
    }

    /**
     * Hashes with BCrypt and stores the algorithm as a prefix: {bcrypt}$2a$10$...
     * The prefix means you can switch algorithms later without breaking existing passwords.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }
        /**
     * Only load scripts, styles and fonts from this site and the jsDelivr CDN (Bootstrap).
     * style-src-attr allows the inline width style on progress bars.
     * frame-ancestors 'none' stops other sites from embedding yours (clickjacking).
     */
    private static final String CONTENT_SECURITY_POLICY = String.join("; ",
            "default-src 'self'",
            "script-src 'self' https://cdn.jsdelivr.net",
            "style-src 'self' https://cdn.jsdelivr.net",
            "style-src-attr 'unsafe-inline'",
            "font-src 'self' https://cdn.jsdelivr.net",
            "img-src 'self' data:",
            "form-action 'self'",
            "frame-ancestors 'none'",
            "base-uri 'self'");
}