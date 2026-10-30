package org.phuchoang.ecp.ordering.internal.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.ordering.internal.application.error.ApplicationException;
import org.phuchoang.ecp.ordering.internal.domain.repository.OrderRepository;

import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * `UC-CUS-10` (`US-CUS-10`): the empty-page stub, and `BR-AUD-02` — the same
 * {@code IdentityAuthorization} call every other module's entry point makes.
 */
@ExtendWith(MockitoExtension.class)
class ListOrdersServiceTest {

    @Mock
    private IdentityAuthorization authorizationService;
    @Mock
    private OrderRepository orders;

    @Test
    void listOrdersAssertsAuthorizationAndReturnsAnEmptyPage() {
        ListOrdersService service = new ListOrdersService(authorizationService, orders);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER"));
        when(orders.findPlacedByCustomerId(caller.accountId(), null, 20))
            .thenReturn(new OrderRepository.OrderPage(java.util.List.of(), null));

        OrderPageResult result = service.listOrders(caller, new ListOrdersQuery(null, 20, null));

        verify(authorizationService).assertAuthorized(caller, ListOrdersService.LIST_ORDERS);
        assertThat(result.items()).isEmpty();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void aCustomerSupplyingCustomerIdIsForbidden() {
        ListOrdersService service = new ListOrdersService(authorizationService, orders);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER"));

        Throwable thrown = catchThrowable(
            () -> service.listOrders(caller, new ListOrdersQuery(null, 20, UUID.randomUUID())));

        assertThat(thrown).isInstanceOf(ApplicationException.class);
    }

    @Test
    void supportMaySupplyCustomerId() {
        ListOrdersService service = new ListOrdersService(authorizationService, orders);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("CUSTOMER_SUPPORT"));

        UUID customerId = UUID.randomUUID();
        when(orders.findPlacedByCustomerId(customerId, null, 20))
            .thenReturn(new OrderRepository.OrderPage(java.util.List.of(), null));
        OrderPageResult result = service.listOrders(caller, new ListOrdersQuery(null, 20, customerId));

        assertThat(result.items()).isEmpty();
    }

    @Test
    void warehouseOperatorReadsPlacedOrders() {
        ListOrdersService service = new ListOrdersService(authorizationService, orders);
        IdentityActor caller = new IdentityActor(UUID.randomUUID(), Set.of("WAREHOUSE_OPERATOR"));
        when(orders.findPlaced(null, 20)).thenReturn(new OrderRepository.OrderPage(java.util.List.of(), null));

        OrderPageResult result = service.listOrders(caller, new ListOrdersQuery(null, 20, null));

        assertThat(result.items()).isEmpty();
        verify(orders).findPlaced(null, 20);
    }
}
