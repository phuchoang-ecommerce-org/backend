package org.phuchoang.ecp.identity.domain;

import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** L1 — `BR-CUS-03` (single-use, time-limited tokens). */
class IdentityTokenTest {

    private final Instant now = Instant.parse("2026-09-12T00:00:00Z");
    private final Clock clock = Clock.fixed(now, ZoneOffset.UTC);

    @Test
    void aFreshlyIssuedTokenIsUsable_BR_CUS_03() {
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(),
            TokenType.EMAIL_VERIFICATION, "hash", Duration.ofHours(24), clock);

        assertThat(token.isUsable(clock)).isTrue();
        assertThat(token.isExpired(clock)).isFalse();
        assertThat(token.isConsumed()).isFalse();
    }

    @Test
    void aTokenPastItsExpiryIsNotUsable_BR_CUS_03() {
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(),
            TokenType.EMAIL_VERIFICATION, "hash", Duration.ofHours(24), clock);
        Clock later = Clock.fixed(now.plus(Duration.ofHours(25)), ZoneOffset.UTC);

        assertThat(token.isExpired(later)).isTrue();
        assertThat(token.isUsable(later)).isFalse();
    }

    @Test
    void aConsumedTokenIsNotUsable_BR_CUS_03() {
        IdentityToken issued = IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(),
            TokenType.EMAIL_VERIFICATION, "hash", Duration.ofHours(24), clock);
        IdentityToken consumed = new IdentityToken(issued.id(), issued.accountId(), issued.type(),
            issued.tokenHash(), issued.issuedAt(), issued.expiresAt(), now, null, issued.chainId());

        assertThat(consumed.isConsumed()).isTrue();
        assertThat(consumed.isUsable(clock)).isFalse();
    }

    @Test
    void aFreshlyIssuedRefreshTokenIsItsOwnChainHead_US_CUS_05() {
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(), TokenType.REFRESH,
            "hash", Duration.ofDays(14), clock);

        assertThat(token.chainId()).isEqualTo(token.id());
    }

    @Test
    void nonRefreshTokensCarryNoChainId_US_CUS_05() {
        IdentityToken token = IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(),
            TokenType.EMAIL_VERIFICATION, "hash", Duration.ofHours(24), clock);

        assertThat(token.chainId()).isNull();
    }

    @Test
    void aRotatedRefreshTokenInheritsTheChainIdOfTheTokenItReplaces_US_CUS_05() {
        IdentityToken original = IdentityToken.issue(UUID.randomUUID(), UUID.randomUUID(), TokenType.REFRESH,
            "hash", Duration.ofDays(14), clock);
        IdentityToken rotated = IdentityToken.rotate(UUID.randomUUID(), original.accountId(), "hash-2",
            Duration.ofDays(14), original.chainId(), clock);

        assertThat(rotated.chainId()).isEqualTo(original.chainId());
        assertThat(rotated.chainId()).isEqualTo(original.id());
    }
}
