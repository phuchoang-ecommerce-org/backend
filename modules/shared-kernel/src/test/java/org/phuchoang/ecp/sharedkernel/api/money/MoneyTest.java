package org.phuchoang.ecp.sharedkernel.api.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class MoneyTest {

    @Test
    void rejectsValuesThatCannotBeStoredInThePlatformMoneyColumns() {
        assertThatThrownBy(() -> new Money(new BigDecimal("1.00001"), "VND"))
            .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "vnd"))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
