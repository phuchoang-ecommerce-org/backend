package org.phuchoang.ecp.identity.internal.application.security;

/**
 * The internal permission port every identity use case calls before executing a command
 * (`ADR-0016` §4, `Domain Model.md` §5.2). Placing the check here, in the application layer, is
 * what makes the decision identical regardless of entry point (`BR-AUD-02`).
 *
 * <p>Other modules reach the same decision through {@code identity.api.IdentityAuthorization};
 * the port lives here because the permission matrix and the roles it reasons about are identity
 * concepts.
 */
public interface PermissionChecker {

    /**
     * @throws org.phuchoang.ecp.sharedkernel.api.error.DomainException {@code ECP-GEN-4030} if
     *     {@code caller}'s roles do not permit {@code operationId} (Permission Matrix.md §5.1)
     */
    void require(CallerContext caller, String operationId);
}
