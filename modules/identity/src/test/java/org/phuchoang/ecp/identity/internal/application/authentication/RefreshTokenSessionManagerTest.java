package org.phuchoang.ecp.identity.internal.application.authentication;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.internal.application.authentication.SessionManager.Presentation;
import org.phuchoang.ecp.identity.internal.application.port.TokenStore;
import org.phuchoang.ecp.identity.internal.application.token.OpaqueTokens;
import org.phuchoang.ecp.identity.internal.domain.model.IdentityToken;
import org.phuchoang.ecp.identity.internal.domain.model.TokenType;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** L1 — the refresh-token protocol in isolation (`ADR-0016` §4). */
@ExtendWith(MockitoExtension.class)
class RefreshTokenSessionManagerTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-12T00:00:00Z"), ZoneOffset.UTC);

    @Mock
    private TokenStore tokenStore;

    @Test
    void issueStoresOnlyTheHashAndStartsANewChain() {
        UUID accountId = UUID.randomUUID();
        RefreshTokenSessionManager sessions = new RefreshTokenSessionManager(tokenStore, clock);

        String raw = sessions.issue(accountId);

        ArgumentCaptor<IdentityToken> saved = ArgumentCaptor.forClass(IdentityToken.class);
        verify(tokenStore).save(saved.capture());
        assertThat(saved.getValue().tokenHash()).isEqualTo(OpaqueTokens.hash(raw)).isNotEqualTo(raw);
        assertThat(saved.getValue().type()).isEqualTo(TokenType.REFRESH);
        assertThat(saved.getValue().chainId()).isEqualTo(saved.getValue().id());
        assertThat(saved.getValue().expiresAt()).isEqualTo(Instant.now(clock).plus(Duration.ofDays(14)));
    }

    @Test
    void presentingAnUnknownTokenIsInvalidWithoutSideEffects() {
        when(tokenStore.findByTokenHash(any(), eq(TokenType.REFRESH))).thenReturn(Optional.empty());

        Presentation presentation = new RefreshTokenSessionManager(tokenStore, clock).present("nope");

        assertThat(presentation).isInstanceOf(Presentation.Invalid.class);
        verify(tokenStore, never()).invalidateChain(any(), any());
    }

    @Test
    void presentingAConsumedTokenEndsTheChainImmediately() {
        IdentityToken issued = usable("raw");
        IdentityToken consumed = new IdentityToken(issued.id(), issued.accountId(), issued.type(), issued.tokenHash(),
            issued.issuedAt(), issued.expiresAt(), Instant.now(clock), UUID.randomUUID(), issued.chainId());
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raw"), TokenType.REFRESH)).thenReturn(Optional.of(consumed));

        Presentation presentation = new RefreshTokenSessionManager(tokenStore, clock).present("raw");

        assertThat(presentation).isEqualTo(new Presentation.Reused(consumed.accountId()));
        verify(tokenStore).invalidateChain(consumed.chainId(), Instant.now(clock));
    }

    @Test
    void presentingAnExpiredTokenIsInvalidAndLeavesTheChainIntact() {
        IdentityToken issued = usable("raw");
        IdentityToken expired = new IdentityToken(issued.id(), issued.accountId(), issued.type(), issued.tokenHash(),
            issued.issuedAt().minus(Duration.ofDays(30)), issued.issuedAt().minus(Duration.ofDays(16)), null, null,
            issued.chainId());
        when(tokenStore.findByTokenHash(OpaqueTokens.hash("raw"), TokenType.REFRESH)).thenReturn(Optional.of(expired));

        Presentation presentation = new RefreshTokenSessionManager(tokenStore, clock).present("raw");

        assertThat(presentation).isInstanceOf(Presentation.Invalid.class);
        verify(tokenStore, never()).invalidateChain(any(), any());
    }

    @Test
    void rotationSavesTheReplacementBeforeConsumingThePresentedToken() {
        IdentityToken presented = usable("raw");
        when(tokenStore.rotateIfUsable(eq(presented.id()), any(), eq(Instant.now(clock)))).thenReturn(true);
        RefreshTokenSessionManager sessions = new RefreshTokenSessionManager(tokenStore, clock);

        Optional<String> rotated = sessions.rotate(
            new Presentation.Usable(presented.accountId(), presented.id(), presented.chainId()));

        assertThat(rotated).isPresent();
        ArgumentCaptor<IdentityToken> saved = ArgumentCaptor.forClass(IdentityToken.class);
        var order = org.mockito.Mockito.inOrder(tokenStore);
        order.verify(tokenStore).save(saved.capture());
        order.verify(tokenStore).rotateIfUsable(presented.id(), saved.getValue().id(), Instant.now(clock));
        assertThat(saved.getValue().chainId()).isEqualTo(presented.chainId());
        assertThat(saved.getValue().tokenHash()).isEqualTo(OpaqueTokens.hash(rotated.get()));
    }

    @Test
    void aLostRotationRaceEndsTheChainAndReturnsNothing() {
        IdentityToken presented = usable("raw");
        when(tokenStore.rotateIfUsable(eq(presented.id()), any(), eq(Instant.now(clock)))).thenReturn(false);

        Optional<String> rotated = new RefreshTokenSessionManager(tokenStore, clock).rotate(
            new Presentation.Usable(presented.accountId(), presented.id(), presented.chainId()));

        assertThat(rotated).isEmpty();
        verify(tokenStore).invalidateChain(presented.chainId(), Instant.now(clock));
    }

    @Test
    void revokeAllInvalidatesEveryOutstandingRefreshToken() {
        UUID accountId = UUID.randomUUID();

        new RefreshTokenSessionManager(tokenStore, clock).revokeAll(accountId);

        verify(tokenStore).invalidateOutstanding(accountId, TokenType.REFRESH);
    }

    private IdentityToken usable(String raw) {
        return IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(), TokenType.REFRESH, OpaqueTokens.hash(raw),
            Duration.ofDays(14), clock);
    }
}
