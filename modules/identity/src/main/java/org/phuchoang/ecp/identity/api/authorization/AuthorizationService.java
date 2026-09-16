package org.phuchoang.ecp.identity.api.authorization;

/**
 * The {@code AuthorizationService} Open Host Service (`US-AUD-03`, `ADR-0016` §4,
 * `Domain Model.md` §5.2), exposed for every other module's <strong>application</strong> layer to
 * call before executing a command — the identical decision reached regardless of entry point
 * (`BR-AUD-02`), because a controller, a scheduled job, and a Kafka consumer all reach business
 * logic through an application service, and this is the one place the rule is enforced.
 *
 * <p>This answers only "does this role reach this operation at all" (`Permission Matrix.md`
 * §5.1's role columns) — never "does this caller own this specific resource." Ownership is a
 * second, separate check the resource's own application service performs itself, after this one
 * passes: it loads the resource and compares it against {@code caller.accountId()}, and on
 * mismatch throws {@code NOT_FOUND} directly — never a second call here, and never {@code
 * FORBIDDEN} — so that another customer's resource is reported identically to one that does not
 * exist (`Integration Contract.md` §2.1). A denial from this service is always {@code FORBIDDEN}
 * ({@code ECP-GEN-4030}); an ownership mismatch is always {@code NOT_FOUND} ({@code
 * ECP-GEN-4040}) and never reaches this service to be told apart.
 *
 * <p>An {@code ArchitectureTests} rule confines references to anything under {@code identity..}
 * to another module's {@code application} package — a {@code domain} object asking who the
 * caller is would make authorisation a business invariant, which {@code Domain Model.md} §5.2
 * forbids.
 */
public interface AuthorizationService {

    /**
     * @throws org.phuchoang.ecp.sharedkernel.api.DomainException {@code ECP-GEN-4030} if
     *     {@code caller}'s roles do not permit {@code operationId} (`Permission Matrix.md` §5.1)
     */
    void assertAuthorized(CallerContext caller, String operationId);
}
