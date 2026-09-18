package org.phuchoang.ecp.ordering.internal.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;

/**
 * `UC-CUS-10` (`US-CUS-10`): the empty-page stub, and `BR-AUD-02` — the same
 * {@code IdentityAuthorization} call every other module's entry point makes.
 */
@ExtendWith(MockitoExtension.class)
class ListOrdersServiceTest {

    @Mock
    private IdentityAuthorization authorizationService;

    @Test
    void listOrdersAssertsAuthorizationAndReturnsAnEmptyPage() {
        ListOrdersService service = new ListOrdersService(authorizationService);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER"));

        OrderPageResult result = service.listOrders(caller, new ListOrdersQuery(null, 20, null));

        verify(authorizationService).assertAuthorized(caller, ListOrdersService.LIST_ORDERS);
        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void aCustomerSupplyingCustomerIdIsForbidden() {
        ListOrdersService service = new ListOrdersService(authorizationService);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER"));

        Throwable thrown = catchThrowable(
            () -> service.listOrders(caller, new ListOrdersQuery(null, 20, UUID.randomUUID())));

        assertThat(thrown).isInstanceOf(DomainException.class);
    }

    @Test
    void supportMaySupplyCustomerId() {
        ListOrdersService service = new ListOrdersService(authorizationService);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER_SUPPORT"));

        OrderPageResult result = service.listOrders(caller, new ListOrdersQuery(null, 20, UUID.randomUUID()));

        assertThat(result.items()).isEmpty();
    }
}
