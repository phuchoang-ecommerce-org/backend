package org.phuchoang.ecp.identity.internal.infrastructure.persistence.token;

import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/** JPA implementation of single-use token operations; conditional updates preserve refresh-token race safety. */
@Repository
class JpaTokenStoreAdapter implements TokenStore {

    private final TokenJpaStore tokenJpaRepository;
    private final Clock clock;

    JpaTokenStoreAdapter(TokenJpaStore tokenJpaRepository, Clock clock) {
        this.tokenJpaRepository = tokenJpaRepository;
        this.clock = clock;
    }

    @Override
    public IdentityToken save(IdentityToken token) {
        TokenEntity entity = new TokenEntity(token.id(), token.accountId(), token.type(), token.tokenHash(),
            token.issuedAt(), token.expiresAt(), token.consumedAt(), token.replacedBy(), token.chainId());
        TokenEntity saved = tokenJpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<IdentityToken> findByTokenHash(String tokenHash, TokenType type) {
        return tokenJpaRepository.findByTokenHashAndTokenType(tokenHash, type).map(this::toDomain);
    }

    @Override
    @Transactional
    public boolean consumeIfUsable(UUID tokenId, Instant now) {
        return tokenJpaRepository.consumeIfUsable(tokenId, now) == 1;
    }

    @Override
    @Transactional
    public boolean rotateIfUsable(UUID tokenId, UUID replacedByTokenId, Instant now) {
        return tokenJpaRepository.rotateIfUsable(tokenId, replacedByTokenId, now) == 1;
    }

    @Override
    @Transactional
    public void invalidateOutstanding(UUID accountId, TokenType type) {
        tokenJpaRepository.invalidateOutstanding(accountId, type, Instant.now(clock));
    }

    @Override
    @Transactional
    public void invalidateChain(UUID chainId, Instant now) {
        tokenJpaRepository.invalidateChain(chainId, now);
    }

    private IdentityToken toDomain(TokenEntity entity) {
        return new IdentityToken(entity.getId(), entity.getAccountId(), entity.getTokenType(),
            entity.getTokenHash(), entity.getIssuedAt(), entity.getExpiresAt(), entity.getConsumedAt(),
            entity.getReplacedBy(), entity.getChainId());
    }
}
