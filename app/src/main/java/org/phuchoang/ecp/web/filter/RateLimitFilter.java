package org.phuchoang.ecp.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.ratelimit.RateLimiter;
import org.phuchoang.ecp.web.error.Problem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Rate limiting, applied before Spring Security's filter chain (Backend Architecture.md §5.8):
 * the auth-strict bucket is checked <em>before</em> credentials are evaluated, which is what
 * makes it a brute-force defence rather than a throttle (`NFR-SEC-05`). Correct credentials never
 * bypass it.
 *
 * <p>Every request is classified into one of four buckets (`US-AUD-04`, Sprint 04): the
 * pre-authentication {@code auth-strict}/{@code payment-retry} paths (fail closed — a silently
 * unavailable limiter must never look like "no limit"), {@code read} for {@code GET}, and
 * {@code write} for every other state-changing method. For an authenticated caller, the bucket is
 * keyed by the access token's {@code sub} claim rather than the remote address — decoded directly
 * here, since this filter must still run <em>ahead of</em> Spring Security (for the pre-auth
 * buckets) and so cannot rely on the security context being populated yet. A caller with no
 * bearer token, or a token that fails to decode (it will 401 downstream regardless), falls back
 * to the remote address.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final Set<String> AUTH_STRICT_PATHS = Set.of(
        "/api/v1/accounts", "/api/v1/account-verification-requests", "/api/v1/sessions",
        "/api/v1/session-renewals", "/api/v1/password-reset-requests", "/api/v1/password-resets");

    /** Empty until a `payment` module controller exists (Sprint 04 scope note, `US-AUD-04`). */
    private static final Set<String> PAYMENT_RETRY_PATHS = Set.of();

    private static final Set<String> FAIL_CLOSED_BUCKETS = Set.of("auth-strict", "payment-retry");

    private final RateLimiter rateLimiter;
    private final JwtDecoder jwtDecoder;

    public RateLimitFilter(RateLimiter rateLimiter, JwtDecoder jwtDecoder) {
        this.rateLimiter = rateLimiter;
        this.jwtDecoder = jwtDecoder;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String bucket = bucketFor(request);
        String callerId = callerIdOf(request);

        RateLimiter.Decision decision;
        try {
            decision = rateLimiter.tryConsume(bucket, callerId);
        } catch (RateLimiter.RateLimiterUnavailableException e) {
            if (FAIL_CLOSED_BUCKETS.contains(bucket)) {
                // ADR-0015 §4 / Backend Architecture.md §5.8 — auth-strict/payment-retry fail
                // CLOSED: a silently-unavailable limiter must never look like "no limit".
                log.warn("redis-state unavailable for {} bucket; failing closed", bucket, e);
                writeProblem(response, GenErrorCode.DEPENDENCY_UNAVAILABLE, "This request is temporarily unavailable.");
                return;
            }
            log.warn("redis-state unavailable for {} bucket; failing open", bucket, e);
            chain.doFilter(request, response);
            return;
        }

        if (!decision.allowed()) {
            response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
            writeProblem(response, GenErrorCode.RATE_LIMITED, "Rate limit exceeded. Retry after the given interval.");
            return;
        }

        chain.doFilter(request, response);
    }

    private static String bucketFor(HttpServletRequest request) {
        if ("POST".equalsIgnoreCase(request.getMethod()) && AUTH_STRICT_PATHS.contains(request.getRequestURI())) {
            return "auth-strict";
        }
        if (PAYMENT_RETRY_PATHS.contains(request.getRequestURI())) {
            return "payment-retry";
        }
        if (HttpMethod.GET.matches(request.getMethod())) {
            return "read";
        }
        return "write";
    }

    private String callerIdOf(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                Jwt jwt = jwtDecoder.decode(header.substring(7));
                return jwt.getSubject();
            } catch (JwtException e) {
                // Falls through to the client address — an invalid/expired token 401s downstream
                // regardless; this filter only needs *a* caller id to key the bucket by.
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * Written by hand rather than through a Jackson {@code ObjectMapper}: this filter runs ahead
     * of the DispatcherServlet (and Spring Security), so the usual message-converter machinery
     * isn't on the request path yet, and the shape here is small and fixed — the same
     * {@code Problem} contract {@link GlobalExceptionHandler} produces (`common.yaml#/Problem`).
     */
    private void writeProblem(HttpServletResponse response, GenErrorCode errorCode, String detail) throws IOException {
        response.setStatus(errorCode.httpStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        String correlationId = CorrelationIdFilter.currentCorrelationId();
        String json = """
            {"type":"%s","title":"%s","status":%d,"code":"%s","detail":"%s","instance":"","correlationId":"%s","errors":[]}"""
            .formatted(
                escape("https://ecp.example/errors/" + errorCode.code()),
                escape(errorCode.title()),
                errorCode.httpStatus(),
                escape(errorCode.code()),
                escape(detail),
                escape(correlationId == null ? "" : correlationId));
        response.getWriter().write(json);
    }

    private static String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
