package org.phuchoang.ecp.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Stateless bearer authentication (`ADR-0016` §4). No cookie, no CSRF filter — this backend is
 * always a JSON API; {@code ADR-0025}'s cookie is the Next.js server's concern, outside this repo
 * (see Sprint 03 plan's "Key design decisions"). This is a coarse, web-layer-only gate
 * (authenticated vs not) — the actual per-operation decision is `identity`'s
 * {@code AuthorizationService}, called from the application layer (`ADR-0016` §3 Option A).
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(HttpMethod.GET, "/healthz").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/accounts").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/account-verifications").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/account-verification-requests").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/sessions").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/v1/session-renewals").permitAll()
                // Sprint 04 (US-AUD-03) RBAC wiring demo — getProduct permits GUEST per the
                // permission matrix, so it must be reachable unauthenticated too.
                .requestMatchers(HttpMethod.GET, "/api/v1/demo-products/*").permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    /** Maps the `roles` claim (`Security.md` §4.2) to `ROLE_*` authorities — nothing else in the token grants authority. */
    private JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(SecurityConfig::rolesClaimToAuthorities);
        return converter;
    }

    private static Collection<GrantedAuthority> rolesClaimToAuthorities(Jwt jwt) {
        Collection<String> roles = jwt.getClaimAsStringList("roles");
        if (roles == null) {
            return List.of();
        }
        return roles.stream().map(role -> (GrantedAuthority) new SimpleGrantedAuthority("ROLE_" + role))
            .collect(Collectors.toUnmodifiableList());
    }
}
