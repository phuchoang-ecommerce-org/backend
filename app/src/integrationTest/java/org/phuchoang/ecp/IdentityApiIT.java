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
        registry.add("ecp.cursor.active-key", () -> "integration-test-cursor-secret-0001");
        // Every test method in this class shares one Spring context, one Redis instance, and one
        // caller address (TestRestTemplate's loopback) — the production auth-strict limit (10 per
        // 5 minutes, NFR-SEC-05) exists to stop credential-guessing from one caller, not to bound
        // how many of *this suite's* tests may register/log in/renew a session. Raised here only,
        // never in application.yml, so the real defence stays exactly as configured (US-AUD-04).
        registry.add("ecp.rate-limit.auth-strict.limit", () -> 1000);
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

    @Test
    void renewSessionRotatesTheRefreshToken_US_CUS_05() {
        String email = "renew-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/accounts",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Void.class);
        Map<?, ?> session = restTemplate.postForEntity("/api/v1/sessions",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Map.class).getBody();
        String originalRefreshToken = (String) session.get("refreshToken");

        ResponseEntity<Map> renewed = restTemplate.postForEntity("/api/v1/session-renewals",
            jsonBody(Map.of("refreshToken", originalRefreshToken)), Map.class);

        assertThat(renewed.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(renewed.getBody()).containsKey("accessToken");
        String newRefreshToken = (String) renewed.getBody().get("refreshToken");
        assertThat(newRefreshToken).isNotEqualTo(originalRefreshToken);
    }

    @Test
    void reusingAnAlreadyRotatedRefreshTokenInvalidatesTheWholeChain_ADR_0016() {
        String email = "reuse-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/accounts",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Void.class);
        Map<?, ?> session = restTemplate.postForEntity("/api/v1/sessions",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Map.class).getBody();
        String originalRefreshToken = (String) session.get("refreshToken");

        Map<?, ?> firstRenewal = restTemplate.postForEntity("/api/v1/session-renewals",
            jsonBody(Map.of("refreshToken", originalRefreshToken)), Map.class).getBody();
        String rotatedRefreshToken = (String) firstRenewal.get("refreshToken");

        // Reusing the now-consumed original token is rejected...
        ResponseEntity<Map> reuseAttempt = restTemplate.postForEntity("/api/v1/session-renewals",
            jsonBody(Map.of("refreshToken", originalRefreshToken)), Map.class);
        assertThat(reuseAttempt.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(reuseAttempt.getBody().get("code")).isEqualTo("ECP-GEN-4011");

        // ...and the reuse invalidates the whole chain: even the legitimately-rotated token that
        // replaced it no longer works.
        ResponseEntity<Map> rotatedTokenAfterReuse = restTemplate.postForEntity("/api/v1/session-renewals",
            jsonBody(Map.of("refreshToken", rotatedRefreshToken)), Map.class);
        assertThat(rotatedTokenAfterReuse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void logOutInvalidatesTheChainSoARotatedTokenFromItAlsoStopsWorking_US_CUS_05() {
        String email = "logout-chain-" + UUID.randomUUID() + "@example.com";
        restTemplate.postForEntity("/api/v1/accounts",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Void.class);
        Map<?, ?> session = restTemplate.postForEntity("/api/v1/sessions",
            jsonBody(Map.of("email", email, "password", "Str0ngPassword")), Map.class).getBody();
        String accessToken = (String) session.get("accessToken");
        String refreshToken = (String) session.get("refreshToken");

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        headers.setContentType(MediaType.APPLICATION_JSON);
        restTemplate.exchange("/api/v1/sessions/current", HttpMethod.DELETE,
            new HttpEntity<>("{\"refreshToken\":\"" + refreshToken + "\"}", headers), Void.class);

        ResponseEntity<Map> renewAfterLogout = restTemplate.postForEntity("/api/v1/session-renewals",
            jsonBody(Map.of("refreshToken", refreshToken)), Map.class);

        assertThat(renewAfterLogout.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
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
