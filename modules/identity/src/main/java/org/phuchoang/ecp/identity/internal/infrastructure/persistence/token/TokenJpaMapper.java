package org.phuchoang.ecp.identity.internal.infrastructure.persistence.token;

import org.mapstruct.Mapper;
import org.mapstruct.AfterMapping;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.ReportingPolicy;
import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;

import java.time.Instant;
import java.util.UUID;

/** Maps token persistence state; conditional consume and rotate operations stay in the store adapter. */
@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.ERROR)
interface TokenJpaMapper {

    TokenEntity toEntity(TokenState source);

    @Mapping(target = "type", source = "tokenType")
    IdentityToken toDomain(TokenEntity source);

    default TokenEntity toEntity(IdentityToken token) {
        return toEntity(TokenState.from(token));
    }

    @AfterMapping
    default void initializeCreatedAt(TokenState source, @MappingTarget TokenEntity target) {
        target.initializeCreatedAt(source.issuedAt());
    }

    record TokenState(UUID id, UUID accountId, TokenType tokenType, String tokenHash, Instant issuedAt,
                      Instant expiresAt, Instant consumedAt, UUID replacedBy, UUID chainId) {
        static TokenState from(IdentityToken token) {
            return new TokenState(token.id(), token.accountId(), token.type(), token.tokenHash(), token.issuedAt(),
                token.expiresAt(), token.consumedAt(), token.replacedBy(), token.chainId());
        }
    }
}
