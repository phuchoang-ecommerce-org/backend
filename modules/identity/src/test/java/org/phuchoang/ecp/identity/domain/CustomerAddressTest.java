package org.phuchoang.ecp.identity.domain;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.sharedkernel.api.address.Address;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/** L1 — no Spring context. `BR-CUS-05` cross-row uniqueness is an application/database concern. */
class CustomerAddressTest {

    private static Address anAddress() {
        return new Address(null, "Jane Doe", "1 Main St", null, "Springfield", null, "12345", "US", null);
    }

    @Test
    void addCreatesAnAddressOwnedByTheGivenAccount() {
        UUID accountId = UUID.randomUUID();
        CustomerAddress address = CustomerAddress.add(UUID.randomUUID(), accountId, anAddress(), true, false);

        assertThat(address.isOwnedBy(accountId)).isTrue();
        assertThat(address.isOwnedBy(UUID.randomUUID())).isFalse();
        assertThat(address.defaultShipping()).isTrue();
        assertThat(address.defaultBilling()).isFalse();
    }

    @Test
    void replaceOverwritesTheAddressAndDefaultFlags_UC_CUS_09_A1() {
        CustomerAddress address = CustomerAddress.add(UUID.randomUUID(), UUID.randomUUID(), anAddress(), false,
            false);
        Address replacement = new Address("Office", "John Doe", "2 Side St", "Suite 4", "Metropolis", "NY", "54321",
            "US", "555-0100");

        address.replace(replacement, true, true);

        assertThat(address.address()).isEqualTo(replacement);
        assertThat(address.defaultShipping()).isTrue();
        assertThat(address.defaultBilling()).isTrue();
    }

    @Test
    void clearDefaultShippingAndBillingUnsetTheFlags() {
        CustomerAddress address = CustomerAddress.add(UUID.randomUUID(), UUID.randomUUID(), anAddress(), true, true);

        address.clearDefaultShipping();
        address.clearDefaultBilling();

        assertThat(address.defaultShipping()).isFalse();
        assertThat(address.defaultBilling()).isFalse();
    }
}
