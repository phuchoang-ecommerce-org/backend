package org.phuchoang.ecp.identity.internal.domain.policy;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AddressBookPolicyTest {
    private final AddressBookPolicy policy = new AddressBookPolicy();

    @Test
    void makesTheFirstAddressTheShippingDefault() {
        var plan = policy.plan(new AddressBookPolicy.AddressBookContext(0, null, false),
            AddressBookPolicy.AddressBookChange.add(false));
        assertThat(plan.accepted()).isTrue();
        assertThat(plan.targetDefaultShipping()).isTrue();
    }

    @Test
    void requiresAReplacementBeforeRemovingTheCurrentDefaultFromANonEmptyBook() {
        var plan = policy.plan(new AddressBookPolicy.AddressBookContext(2, UUID.randomUUID(), true),
            new AddressBookPolicy.AddressBookChange(AddressBookPolicy.ChangeType.REMOVE, UUID.randomUUID(), false));
        assertThat(plan.accepted()).isFalse();
        assertThat(plan.reason()).isEqualTo(AddressBookPolicy.Reason.REPLACEMENT_DEFAULT_REQUIRED);
    }
}
