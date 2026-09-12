package org.phuchoang.ecp.catalog.application;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phuchoang.ecp.identity.api.AuthorizationService;
import org.phuchoang.ecp.identity.api.CallerContext;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

/** L1 — US-AUD-03 RBAC wiring demo: proves the service calls AuthorizationService before acting. */
@ExtendWith(MockitoExtension.class)
class GetProductServiceTest {

    @Mock
    private AuthorizationService authorizationService;

    @Test
    void getProductAssertsAuthorizationBeforeReturningTheStubView() {
        GetProductService service = new GetProductService(authorizationService);
        UUID productId = UUID.randomUUID();

        ProductView view = service.getProduct(CallerContext.GUEST, productId);

        verify(authorizationService).assertAuthorized(CallerContext.GUEST, GetProductService.GET_PRODUCT);
        assertThat(view.id()).isEqualTo(productId);
    }
}
