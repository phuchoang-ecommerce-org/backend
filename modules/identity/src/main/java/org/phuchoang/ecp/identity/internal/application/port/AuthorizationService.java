package org.phuchoang.ecp.identity.internal.application.port;

import org.phuchoang.ecp.identity.internal.application.CallerContext;

/**
 * The `AuthorizationService` Open Host Service (`ADR-0016` §4, `Domain Model.md` §5.2): every
 * `@ApplicationService` — in this module and, once other modules exist, in theirs — calls this
 * before executing a command. Placing the check here, in the application layer, is what makes the
 * decision identical regardless of entry point (`BR-AUD-02`) — a controller, a scheduled job, and
 * a Kafka consumer all reach business logic through an application service, so there is exactly
 * one place the rule can be enforced.
 *
 * <p>Other modules will reach this through {@code identity.api}'s facade once they exist
 * (Sprint 03 only wires identity's own five operations); the interface lives here because the
 * permission matrix and the roles it reasons about are identity concepts.
 */
public interface AuthorizationService {

    /**
     * @throws org.phuchoang.ecp.sharedkernel.api.error.DomainException {@code ECP-GEN-4030} if
     *     {@code caller}'s roles do not permit {@code operationId} (Permission Matrix.md §5.1)
     */
    void assertAuthorized(CallerContext caller, String operationId);
}
