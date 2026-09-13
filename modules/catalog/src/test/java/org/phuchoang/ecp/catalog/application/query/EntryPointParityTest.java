package org.phuchoang.ecp.catalog.application.query;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.catalog.api.GetProductFacade;
import org.phuchoang.ecp.catalog.infrastructure.event.ProductLookupEventHandler;
import org.phuchoang.ecp.catalog.infrastructure.scheduling.ProductLookupScheduledTask;
import org.phuchoang.ecp.identity.api.AuthorizationService;
import org.phuchoang.ecp.identity.api.CallerContext;

import java.util.UUID;

import static org.mockito.Mockito.verify;

/**
 * `BR-AUD-02` (`US-AUD-03`): the same authorisation decision is reached whichever entry point
 * calls {@link GetProductService} — REST ({@link GetProductFacade}, standing in for
 * {@code ProductController}), a scheduler-shaped stub, and a Kafka-consumer-shaped stub. No real
 * scheduler or Kafka infrastructure exists in this codebase yet (Sprint 04 Review Notes) — each
 * test below calls its entry point's method directly, proving the call path converges on one
 * identical {@code AuthorizationService.assertAuthorized(caller, "getProduct")} call, not that a
 * trigger or broker fired it.
 */
@ExtendWith(MockitoExtension.class)
class EntryPointParityTest {

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void theRestPathAuthorizesTheSameWayAsTheOtherEntryPoints() {
        GetProductService service = new GetProductService(authorizationService);
        GetProductFacade facade = new GetProductFacade(service);

        facade.getProduct(CallerContext.GUEST, UUID.randomUUID());

        verify(authorizationService).assertAuthorized(CallerContext.GUEST, GetProductService.GET_PRODUCT);
    }

    @Test
    void theSchedulerShapedPathAuthorizesTheSameWayAsTheRestPath() {
        GetProductService service = new GetProductService(authorizationService);
        ProductLookupScheduledTask scheduledTask = new ProductLookupScheduledTask(service);

        scheduledTask.run();

        verify(authorizationService).assertAuthorized(CallerContext.GUEST, GetProductService.GET_PRODUCT);
    }

    @Test
    void theKafkaShapedPathAuthorizesTheSameWayAsTheRestPath() {
        GetProductService service = new GetProductService(authorizationService);
        ProductLookupEventHandler eventHandler = new ProductLookupEventHandler(service);

        eventHandler.handle(UUID.randomUUID());

        verify(authorizationService).assertAuthorized(CallerContext.GUEST, GetProductService.GET_PRODUCT);
    }
}
