package org.phuchoang.ecp.sharedkernel.api.ratelimit;

/**
 * A caller/bucket sliding-window limiter, backed by {@code redis-state} (ADR-0034 §5.8). Declared
 * here as a plain-Java port so any module's application layer can depend on it without pulling in
 * a Redis client type — shared-kernel carries no Spring dependency (Module Dependency Diagram.md §7).
 * The concrete adapter lives in a module's {@code infrastructure} package.
 */
public interface RateLimiter {

    /**
     * Consumes one unit of the given bucket for the given caller.
     *
     * @param bucket   the rate-limit bucket name, e.g. {@code "auth-strict"} or {@code "write"}
     *                 (Backend Architecture.md §5.8)
     * @param callerId an identifier for the caller (account id, or an IP/email for an
     *                 unauthenticated caller)
     * @return the outcome — whether the call may proceed, and if not, when to retry
     * @throws RateLimiterUnavailableException if the backing store cannot be reached or times out;
     *                                          the caller decides fail-open vs fail-closed per bucket
     */
    Decision tryConsume(String bucket, String callerId);

    record Decision(boolean allowed, long retryAfterSeconds) {
    }

    class RateLimiterUnavailableException extends RuntimeException {
        public RateLimiterUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
