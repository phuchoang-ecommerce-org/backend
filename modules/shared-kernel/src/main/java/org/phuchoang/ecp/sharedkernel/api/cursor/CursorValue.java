package org.phuchoang.ecp.sharedkernel.api.cursor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;

/** A typed value in an ordered keyset position. */
public record CursorValue(Type type, String value) {

    public enum Type { TEXT, DECIMAL, INSTANT, INTEGER, NULL }

    public CursorValue {
        Objects.requireNonNull(type, "type");
        if (type == Type.NULL) {
            if (value != null) {
                throw new IllegalArgumentException("A null cursor value must not have content.");
            }
        } else if (value == null) {
            throw new IllegalArgumentException("A cursor value must have content.");
        }
    }

    public static CursorValue text(String value) {
        return new CursorValue(Type.TEXT, value);
    }

    public static CursorValue decimal(BigDecimal value) {
        return value == null ? nullValue() : new CursorValue(Type.DECIMAL, value.toPlainString());
    }

    public static CursorValue instant(Instant value) {
        return new CursorValue(Type.INSTANT, Objects.requireNonNull(value, "value").toString());
    }

    public static CursorValue integer(int value) {
        return new CursorValue(Type.INTEGER, Integer.toString(value));
    }

    public static CursorValue nullValue() {
        return new CursorValue(Type.NULL, null);
    }

    public String textValue() {
        require(Type.TEXT);
        return value;
    }

    public BigDecimal decimalValue() {
        require(Type.DECIMAL);
        return new BigDecimal(value);
    }

    public Instant instantValue() {
        require(Type.INSTANT);
        return Instant.parse(value);
    }

    public int integerValue() {
        require(Type.INTEGER);
        return Integer.parseInt(value);
    }

    private void require(Type expected) {
        if (type != expected) {
            throw new InvalidCursorException();
        }
    }
}
