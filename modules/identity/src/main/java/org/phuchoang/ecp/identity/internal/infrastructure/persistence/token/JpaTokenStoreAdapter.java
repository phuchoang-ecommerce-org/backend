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
    private final TokenJpaMapper mapper;

    JpaTokenStoreAdapter(TokenJpaStore tokenJpaRepository, Clock clock, TokenJpaMapper mapper) {
        this.tokenJpaRepository = tokenJpaRepository;
        this.clock = clock;
        this.mapper = mapper;
    }

    @Override
    public IdentityToken save(IdentityToken token) {
        return mapper.toDomain(tokenJpaRepository.save(mapper.toEntity(token)));
    }

    @Override
    public Optional<IdentityToken> findByTokenHash(String tokenHash, TokenType type) {
        return tokenJpaRepository.findByTokenHashAndTokenType(tokenHash, type).map(mapper::toDomain);
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

}
