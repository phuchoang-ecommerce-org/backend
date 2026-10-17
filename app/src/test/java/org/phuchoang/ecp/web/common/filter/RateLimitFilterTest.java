package org.phuchoang.ecp.web.common.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.web.common.ratelimit.RateLimiter;
import org.phuchoang.ecp.web.common.filter.RateLimitFilter;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;

/**
 * L2 — `US-AUD-04`: bucket classification (auth-strict paths, `GET` -> `read`, else `write`) and
 * per-caller keying (JWT `sub` for an authenticated caller, remote address otherwise).
 */
@ExtendWith(MockitoExtension.class)
class RateLimitFilterTest {

    @Mock
    private RateLimiter rateLimiter;
    @Mock
    private JwtDecoder jwtDecoder;
    @Mock
    private FilterChain filterChain;

    private RateLimitFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitFilter(rateLimiter, jwtDecoder);
        when(rateLimiter.tryConsume(anyString(), anyString())).thenReturn(new RateLimiter.Decision(true, 0));
    }

    @Test
    void aGetRequestIsClassifiedIntoTheReadBucket() throws Exception {
        HttpServletRequest request = request("GET", "/api/v1/accounts", null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiter).tryConsume(eq("read"), anyString());
    }

    @Test
    void aPostToAnAuthStrictPathIsClassifiedAuthStrictEvenWithoutABearerToken() throws Exception {
        HttpServletRequest request = request("POST", "/api/v1/sessions", null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiter).tryConsume(eq("auth-strict"), anyString());
    }

    @Test
    void aPostToAnOrdinaryPathIsClassifiedWrite() throws Exception {
        HttpServletRequest request = request("POST", "/api/v1/some-other-resource", null);

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiter).tryConsume(eq("write"), anyString());
    }

    @Test
    void paymentRetryUsesTheFraudProtectionBucketAndFailsClosed() throws Exception {
        when(rateLimiter.tryConsume("payment-retry", "203.0.113.1"))
            .thenThrow(new RateLimiter.RateLimiterUnavailableException("unavailable", null));
        HttpServletRequest request = request("POST", "/api/v1/payments/123e4567-e89b-12d3-a456-426614174000/attempts", null);
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilter(request, response, filterChain);

        verify(rateLimiter).tryConsume("payment-retry", "203.0.113.1");
        assertThat(response.getStatus()).isEqualTo(503);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void listingAttemptsDoesNotUseThePaymentRetryBucket() throws Exception {
        filter.doFilter(request("GET", "/api/v1/payments/123e4567-e89b-12d3-a456-426614174000/attempts", null),
            new MockHttpServletResponse(), filterChain);
        verify(rateLimiter).tryConsume("read", "203.0.113.1");
    }

    @Test
    void anAuthenticatedCallerIsKeyedByTheJwtSubjectNotTheRemoteAddress() throws Exception {
        Jwt jwt = new Jwt("raw-token", Instant.now(), Instant.now().plusSeconds(900),
            Map.of("alg", "RS256"), Map.of("sub", "018f3c2a-0000-7000-8000-000000000000"));
        when(jwtDecoder.decode("valid-token")).thenReturn(jwt);
        HttpServletRequest request = request("GET", "/api/v1/accounts", "Bearer valid-token");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiter).tryConsume("read", "018f3c2a-0000-7000-8000-000000000000");
    }

    @Test
    void twoDifferentAccountsFromTheSameRemoteAddressGetIndependentBudgets() throws Exception {
        Jwt jwtA = new Jwt("a", Instant.now(), Instant.now().plusSeconds(900), Map.of("alg", "RS256"),
            Map.of("sub", "account-a"));
        Jwt jwtB = new Jwt("b", Instant.now(), Instant.now().plusSeconds(900), Map.of("alg", "RS256"),
            Map.of("sub", "account-b"));
        when(jwtDecoder.decode("token-a")).thenReturn(jwtA);
        when(jwtDecoder.decode("token-b")).thenReturn(jwtB);

        filter.doFilter(request("GET", "/api/v1/accounts", "Bearer token-a"), new MockHttpServletResponse(),
            filterChain);
        filter.doFilter(request("GET", "/api/v1/accounts", "Bearer token-b"), new MockHttpServletResponse(),
            filterChain);

        verify(rateLimiter).tryConsume("read", "account-a");
        verify(rateLimiter).tryConsume("read", "account-b");
    }

    @Test
    void anInvalidBearerTokenFallsBackToTheRemoteAddress() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new JwtException("bad token"));
        HttpServletRequest request = request("GET", "/api/v1/accounts", "Bearer garbage");

        filter.doFilter(request, new MockHttpServletResponse(), filterChain);

        verify(rateLimiter).tryConsume("read", "203.0.113.1");
    }

    private static HttpServletRequest request(String method, String uri, String authorizationHeader) {
        org.springframework.mock.web.MockHttpServletRequest request =
            new org.springframework.mock.web.MockHttpServletRequest(method, uri);
        request.setRemoteAddr("203.0.113.1");
        if (authorizationHeader != null) {
            request.addHeader("Authorization", authorizationHeader);
        }
        return request;
    }
}
