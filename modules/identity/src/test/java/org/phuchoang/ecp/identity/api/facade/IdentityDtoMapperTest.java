package org.phuchoang.ecp.identity.api.facade;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import org.phuchoang.ecp.identity.api.request.AddressWriteRequest;
import org.phuchoang.ecp.identity.api.request.ChangePasswordRequest;
import org.phuchoang.ecp.identity.api.view.AddressPageView;
import org.phuchoang.ecp.identity.internal.application.address.AddressPageResult;
import org.phuchoang.ecp.identity.internal.application.address.AddressSummary;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class IdentityDtoMapperTest {

    private final IdentityDtoMapper mapper = Mappers.getMapper(IdentityDtoMapper.class);

    @Test
    void mapsTransportDefaultsIntoTheApplicationCommands() {
        var password = mapper.changePasswordCommand(new ChangePasswordRequest("old-password", "new-password", null));
        var address = mapper.addressCommand(new AddressWriteRequest(null, "Jane Doe", "1 Main St", null,
            "Springfield", null, "12345", "US", null, null, null));

        assertThat(password.endOtherSessions()).isTrue();
        assertThat(address.isDefaultShipping()).isFalse();
        assertThat(address.isDefaultBilling()).isFalse();
        assertThat(address.address().recipientName()).isEqualTo("Jane Doe");
    }

    @Test
    void mapsAddressPagesWithoutChangingTheExistingImmutableItemsContract() {
        AddressSummary item = new AddressSummary(UUID.randomUUID().toString(), null, "Jane Doe", "1 Main St", null,
            "Springfield", null, "12345", "US", null, true, false);

        AddressPageView page = mapper.addressPageView(new AddressPageResult(List.of(item), "next-cursor"));

        assertThat(page.nextCursor()).isEqualTo("next-cursor");
        assertThat(page.items()).hasSize(1);
        assertThatThrownBy(() -> page.items().add(page.items().getFirst()))
            .isInstanceOf(UnsupportedOperationException.class);
    }
}
