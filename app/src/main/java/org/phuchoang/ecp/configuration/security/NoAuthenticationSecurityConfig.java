package org.phuchoang.ecp.configuration.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Local manual-testing security configuration. This configuration is deliberately isolated behind
 * the explicit {@code no-auth} profile so a normal or production start continues to require JWTs.
 */
@Configuration
@EnableWebSecurity
@Profile("no-auth & !prod")
public class NoAuthenticationSecurityConfig {

    @Bean
    SecurityFilterChain noAuthenticationSecurityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(authorize -> authorize.anyRequest().permitAll());
        return http.build();
    }
}
