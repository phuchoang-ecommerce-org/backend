package org.phuchoang.ecp.sharedkernel.api;

import org.phuchoang.ecp.sharedkernel.api.cursor.CursorContext;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorPosition;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorSigningKey;
import org.phuchoang.ecp.sharedkernel.api.cursor.CursorValue;
import org.phuchoang.ecp.sharedkernel.api.cursor.HmacCursorCodec;
import org.phuchoang.ecp.sharedkernel.api.cursor.InvalidCursorException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class HmacCursorCodecTest {

    private static final CursorSigningKey OLD_KEY = CursorSigningKey.utf8("cursor-2025",
        "01234567890123456789012345678901");
    private static final CursorSigningKey ACTIVE_KEY = CursorSigningKey.utf8("cursor-2026",
        "abcdefghijklmnopqrstuvwxyzABCDEF");
    private static final CursorContext CONTEXT = new CursorContext("catalog.category-products", "category-a",
        "price:asc", Map.of("brand", "acme", "priceFrom", "10.00"));

    @Test
    void roundTripsTypedSortValuesWithTheActiveKey() {
        CursorCodec codec = new HmacCursorCodec(ACTIVE_KEY, OLD_KEY);
        UUID id = UUID.randomUUID();

        String token = codec.encode(CONTEXT, List.of(CursorValue.text("A product"), CursorValue.decimal(new BigDecimal("12.50")),
            CursorValue.instant(Instant.parse("2026-09-14T12:00:00Z")), CursorValue.integer(7)), id);

        CursorPosition position = codec.decode(token, CONTEXT);
        assertThat(position.tieBreaker()).isEqualTo(id);
        assertThat(position.sortValues()).containsExactly(CursorValue.text("A product"),
            CursorValue.decimal(new BigDecimal("12.50")), CursorValue.instant(Instant.parse("2026-09-14T12:00:00Z")),
            CursorValue.integer(7));
    }

    @Test
    void acceptsPreviousKeyDuringRotationButAFormerActiveKeyCannotIssueNewTokens() {
        CursorCodec beforeRotation = new HmacCursorCodec(OLD_KEY, null);
        String oldToken = beforeRotation.encode(CONTEXT, List.of(CursorValue.integer(3)), UUID.randomUUID());
        CursorCodec duringRotation = new HmacCursorCodec(ACTIVE_KEY, OLD_KEY);

        assertThat(duringRotation.decode(oldToken, CONTEXT).sortValues()).containsExactly(CursorValue.integer(3));
        assertThat(duringRotation.encode(CONTEXT, List.of(CursorValue.integer(4)), UUID.randomUUID()))
            .doesNotContain(oldToken.split("\\.")[0]);
        assertThatThrownBy(() -> new HmacCursorCodec(ACTIVE_KEY, null).decode(oldToken, CONTEXT))
            .isInstanceOf(InvalidCursorException.class);
    }

    @Test
    void rejectsTamperingMalformedPayloadAndContextMismatch() {
        CursorCodec codec = new HmacCursorCodec(ACTIVE_KEY, null);
        String token = codec.encode(CONTEXT, List.of(CursorValue.nullValue()), UUID.randomUUID());
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        assertThatThrownBy(() -> codec.decode(tampered, CONTEXT)).isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode("not.a.cursor", CONTEXT)).isInstanceOf(InvalidCursorException.class);
        assertThatThrownBy(() -> codec.decode(token, new CursorContext("catalog.category-products", "category-b",
            "price:asc", CONTEXT.normalizedFilters()))).isInstanceOf(InvalidCursorException.class);
    }
}
