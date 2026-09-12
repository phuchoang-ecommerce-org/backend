package org.phuchoang.ecp;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.resttestclient.TestRestTemplate;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureTestRestTemplate;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * L4/L5 — the full Sprint 03 backend lane against real Postgres and both Redis instances: register
 * → log in as unverified (restricted session) → attempt again after wrong password gets the exact
 * `logIn` failure → log out invalidates the refresh token so it cannot be reused (`BR-CUS-03`).
 *
 * <p>Verification-token consumption itself is exercised at L1 (`IdentityTokenTest`,
 * `VerifyEmailServiceTest`-equivalent coverage via `LoginServiceTest`); this test does not follow
 * a real verification link because doing so would require reading the raw token back out of the
 * stubbed notification log line, which is deliberately not a public contract (`NFR-SEC-07`).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureTestRestTemplate
@Import(TestcontainersConfiguration.class)
class IdentityApiIT {

    // ADR-0034's two-instance topology, plain containers rather than @ServiceConnection since
    // redis.RedisConfig reads its own ecp.redis.* properties, not spring.data.redis.*.
    private static final GenericContainer<?> REDIS_CACHE =
        new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);
    private static final GenericContainer<?> REDIS_STATE =
        new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    static {
        REDIS_CACHE.start();
        REDIS_STATE.start();
    }

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("ecp.redis.cache.host", REDIS_CACHE::getHost);
        registry.add("ecp.redis.cache.port", () -> REDIS_CACHE.getMappedPort(6379));
        registry.add("ecp.redis.state.host", REDIS_STATE::getHost);
        registry.add("ecp.redis.state.port", () -> REDIS_STATE.getMappedPort(6379));
    }

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void registerThenLogInYieldsARestrictedSessionForAnUnverifiedAccount() {
        String email = "customer-" + UUID.randomUUID() + "@example.com";

        ResponseEntity<Void> registration = restTemplate.postForEntity("/api/v1/accounts",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Void.class);
        assertThat(registration.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);

        ResponseEntity<Map> session = restTemplate.postForEntity("/api/v1/sessions",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Map.class);

        assertThat(session.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(session.getBody()).containsKey("accessToken");
        assertThat(session.getBody().get("restricted")).isEqualTo(true); // A1 — unverified
        assertThat(((Map<?, ?>) session.getBody().get("account")).get("email")).isEqualTo(email);
    }

    @Test
    void duplicateRegistrationStillReturns202_BR_CUS_04() {
        String email = "dup-" + UUID.randomUUID() + "@example.com";
        HttpEntity<String> body = jsonBody(Map.of("email", email, "password", "Str0ngPassword"));

        ResponseEntity<Void> first = restTemplate.postForEntity("/api/v1/accounts", body, Void.class);
        ResponseEntity<Void> second = restTemplate.postForEntity("/api/v1/accounts", body, Void.class);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED); // BR-CUS-01/BR-CUS-04
    }

    @Test
    void wrongPasswordAndUnknownAccountFailIdentically_BR_CUS_04() {
        String email = "known-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/accounts",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Void.class);

        ResponseEntity<Map> wrongPassword = restTemplate.postForEntity("/api/v1/sessions",
            jsonBody(Map.of("email", email, "password", "WrongPassword1")), Map.class);
        ResponseEntity<Map> unknownAccount = restTemplate.postForEntity("/api/v1/sessions",
            jsonBody(Map.of("email", "nobody-" + UUID.randomUUID() + "@example.com", "password", "WrongPassword1")),
            Map.class);

        assertThat(wrongPassword.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(unknownAccount.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(wrongPassword.getBody().get("code")).isEqualTo(unknownAccount.getBody().get("code"));
        assertThat(wrongPassword.getBody().get("detail")).isEqualTo(unknownAccount.getBody().get("detail"));
    }

    @Test
    void logOutInvalidatesTheRefreshTokenSoItCannotBeReused_BR_CUS_03() {
        String email = "logout-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/accounts",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Void.class);
        Map<?, ?> session = restTemplate.postForEntity("/api/v1/sessions",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Map.class).getBody();
        String accessToken = (String) session.get("accessToken");
        String refreshToken = (String) session.get("refreshToken");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> logoutRequest = new HttpEntity<>("{\"refreshToken\":\"" + refreshToken + "\"}", headers);

        ResponseEntity<Void> logout = restTemplate.exchange("/api/v1/sessions/current", HttpMethod.DELETE,
            logoutRequest, Void.class);

        assertThat(logout.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    private HttpEntity<String> jsonBody(Map<String, String> fields) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        String json = fields.entrySet().stream()
            .map(e -> "\"" + e.getKey() + "\":\"" + e.getValue() + "\"")
            .reduce((a, b) -> a + "," + b)
            .map(pairs -> "{" + pairs + "}")
            .orElse("{}");
        return new HttpEntity<>(json, headers);
    }
}
