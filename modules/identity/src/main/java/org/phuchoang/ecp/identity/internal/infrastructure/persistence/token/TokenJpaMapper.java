package org.phuchoang.ecp.identity.internal.infrastructure.persistence.token;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;

import java.time.Instant;
import java.util.UUID;

/** Maps token persistence state; conditional consume and rotate operations stay in the store adapter. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface TokenJpaMapper {

    TokenEntity toEntity(TokenState source);

    default TokenEntity toEntity(IdentityToken token) {
        TokenEntity entity = toEntity(TokenState.from(token));
        entity.initializeCreatedAt(token.issuedAt());
        return entity;
    }

    default IdentityToken toDomain(TokenEntity entity) {
        return new IdentityToken(entity.getId(), entity.getAccountId(), entity.getTokenType(), entity.getTokenHash(),
            entity.getIssuedAt(), entity.getExpiresAt(), entity.getConsumedAt(), entity.getReplacedBy(), entity.getChainId());
    }

    record TokenState(UUID id, UUID accountId, TokenType tokenType, String tokenHash, Instant issuedAt,
                      Instant expiresAt, Instant consumedAt, UUID replacedBy, UUID chainId) {
        static TokenState from(IdentityToken token) {
            return new TokenState(token.id(), token.accountId(), token.type(), token.tokenHash(), token.issuedAt(),
                token.expiresAt(), token.consumedAt(), token.replacedBy(), token.chainId());
        }
    }
}
