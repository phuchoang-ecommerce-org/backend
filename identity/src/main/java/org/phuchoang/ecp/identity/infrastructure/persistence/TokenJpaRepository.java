package org.phuchoang.ecp.identity.infrastructure.persistence;

import org.phuchoang.ecp.identity.domain.TokenType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface TokenJpaRepository extends JpaRepository<TokenEntity, UUID> {

    Optional<TokenEntity> findByTokenHashAndTokenType(String tokenHash, TokenType tokenType);

    List<TokenEntity> findByAccountIdAndTokenTypeAndConsumedAtIsNull(UUID accountId, TokenType tokenType);

    /**
     * The single-use/rotation guarantee (`BR-CUS-03`): a conditional update, arbitrated by the
     * database, never a preceding read (Sequence/04-Identity.md §4/§7). The extra
     * {@code expires_at > :now} means a merely-expired token reports as "not usable" through the
     * same path as an already-consumed one, rather than silently "succeeding" past its expiry.
     */
    @Modifying
    @Query("UPDATE TokenEntity t SET t.consumedAt = :now "
        + "WHERE t.id = :id AND t.consumedAt IS NULL AND t.expiresAt > :now")
    int consumeIfUsable(@Param("id") UUID id, @Param("now") Instant now);

    @Modifying
    @Query("UPDATE TokenEntity t SET t.consumedAt = :now "
        + "WHERE t.accountId = :accountId AND t.tokenType = :type AND t.consumedAt IS NULL")
    int invalidateOutstanding(@Param("accountId") UUID accountId, @Param("type") TokenType type,
        @Param("now") Instant now);
}
