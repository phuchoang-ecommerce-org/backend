package org.phuchoang.ecp.identity.infrastructure.persistence;

import org.phuchoang.ecp.identity.application.port.TokenRepository;
import org.phuchoang.ecp.identity.domain.IdentityToken;
import org.phuchoang.ecp.identity.domain.TokenType;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Repository
class JpaTokenRepositoryAdapter implements TokenRepository {

    private final TokenJpaRepository tokenJpaRepository;
    private final Clock clock;

    JpaTokenRepositoryAdapter(TokenJpaRepository tokenJpaRepository, Clock clock) {
        this.tokenJpaRepository = tokenJpaRepository;
        this.clock = clock;
    }

    @Override
    public IdentityToken save(IdentityToken token) {
        TokenEntity entity = new TokenEntity(token.id(), token.accountId(), token.type(), token.tokenHash(),
            token.issuedAt(), token.expiresAt(), token.consumedAt(), token.replacedBy());
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
    public void invalidateOutstanding(UUID accountId, TokenType type) {
        tokenJpaRepository.invalidateOutstanding(accountId, type, Instant.now(clock));
    }

    private IdentityToken toDomain(TokenEntity entity) {
        return new IdentityToken(entity.getId(), entity.getAccountId(), entity.getTokenType(),
            entity.getTokenHash(), entity.getIssuedAt(), entity.getExpiresAt(), entity.getConsumedAt(),
            entity.getReplacedBy());
    }
}
