package org.phuchoang.ecp.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Stateless bearer authentication (`ADR-0016` §4). No cookie, no CSRF filter — this backend is
 * always a JSON API; {@code ADR-0025}'s cookie is the Next.js server's concern, outside this repo.
 * This is a coarse, web-layer-only gate — "is this endpoint public, operational, or does it need a
 * token" ({@link ApiRoutes}) — the actual per-operation decision is identity's
 * {@code IdentityAuthorization}, called from each bounded context's application layer
 * (`ADR-0016` §3 Option A).
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
                .requestMatchers(HttpMethod.GET, ApiRoutes.OPERATIONAL_GET).permitAll()
                .requestMatchers(HttpMethod.POST, ApiRoutes.PUBLIC_POST).permitAll()
                .requestMatchers(HttpMethod.GET, ApiRoutes.PUBLIC_GET).permitAll()
                .anyRequest().authenticated())
            .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));
        return http.build();
    }

    private static JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(new JwtAuthorityConverter());
        return converter;
    }
}
