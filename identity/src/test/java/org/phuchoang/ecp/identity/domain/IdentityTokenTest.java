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
            issued.tokenHash(), issued.issuedAt(), issued.expiresAt(), now, null);

        assertThat(consumed.isConsumed()).isTrue();
        assertThat(consumed.isUsable(clock)).isFalse();
    }
}
