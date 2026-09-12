package org.phuchoang.ecp.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.phuchoang.ecp.sharedkernel.api.GenErrorCode;
import org.phuchoang.ecp.sharedkernel.api.RateLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
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
 * <p>The caller id is the remote address — there is no authenticated principal yet at this point
 * in the pipeline (this filter runs ahead of Spring Security), and the endpoints in the
 * auth-strict set are all pre-authentication by definition.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RateLimitFilter.class);

    private static final Set<String> AUTH_STRICT_PATHS = Set.of(
        "/api/v1/accounts", "/api/v1/account-verification-requests", "/api/v1/sessions");

    private final RateLimiter rateLimiter;

    public RateLimitFilter(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!"POST".equalsIgnoreCase(request.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        boolean authStrict = AUTH_STRICT_PATHS.contains(request.getRequestURI());
        String bucket = authStrict ? "auth-strict" : "write";
        String callerId = request.getRemoteAddr();

        RateLimiter.Decision decision;
        try {
            decision = rateLimiter.tryConsume(bucket, callerId);
        } catch (RateLimiter.RateLimiterUnavailableException e) {
            if (authStrict) {
                // ADR-0015 §4 / Backend Architecture.md §5.8 — the auth-strict bucket fails
                // CLOSED: a silently-unavailable limiter must never look like "no limit".
                log.warn("redis-state unavailable for auth-strict bucket; failing closed", e);
                writeProblem(response, GenErrorCode.DEPENDENCY_UNAVAILABLE, "Authentication is temporarily unavailable.");
                return;
            }
            log.warn("redis-state unavailable for write bucket; failing open", e);
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
