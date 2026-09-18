package org.phuchoang.ecp.identity.internal.application.authentication;

import java.util.Optional;
import java.util.UUID;

/**
 * The refresh-token session protocol (`ADR-0016` §4): issue, present, rotate, revoke. Owns the
 * token lifetime, hashing, chain bookkeeping and reuse detection so the authentication use cases
 * read as workflows rather than as sequences of {@code TokenStore} calls. Persistence atomicity
 * (single-use consumption, conditional rotation) stays inside {@code TokenStore}; this component
 * orchestrates it, never re-implements it.
 */
public interface SessionManager {

    /** Starts a new chain for {@code accountId}. @return the raw refresh token to hand to the caller. */
    String issue(UUID accountId);

    /**
     * Classifies a presented refresh token. A {@link Presentation.Reused} result has <em>already</em>
     * invalidated the token's whole chain — the caller only reports it.
     */
    Presentation present(String rawRefreshToken);

    /**
     * Rotates a usable presentation: the new token is saved first (the {@code replaced_by} FK is not
     * deferrable), then the old one is consumed-and-linked in one conditional update.
     *
     * @return the new raw refresh token, or empty when a concurrent rotation of the same token won
     *     the race — in which case the chain has already been invalidated, exactly as for reuse
     */
    Optional<String> rotate(Presentation.Usable presented);

    /** Ends the chain {@code rawRefreshToken} belongs to; an unknown, expired, or consumed token is a no-op. */
    void revoke(String rawRefreshToken);

    /** Ends every outstanding session of {@code accountId}. */
    void revokeAll(UUID accountId);

    sealed interface Presentation {

        record Usable(UUID accountId, UUID tokenId, UUID chainId) implements Presentation {
        }

        /** Already consumed — attacker replay or losing side of a double refresh; the chain is now ended. */
        record Reused(UUID accountId) implements Presentation {
        }

        /** Unknown or expired — ordinary rejection; the chain is left intact. */
        record Invalid() implements Presentation {
        }
    }
}
