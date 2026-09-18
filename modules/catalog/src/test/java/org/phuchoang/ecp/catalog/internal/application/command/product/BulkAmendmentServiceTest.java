package org.phuchoang.ecp.catalog.internal.application.command.product;

import org.junit.jupiter.api.Test;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;
import org.phuchoang.ecp.sharedkernel.api.error.GenErrorCode;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Partial success is the contract: each item stands alone and reports its own outcome. */
class BulkAmendmentServiceTest {

    private final UpdateProductService updates = mock(UpdateProductService.class);
    private final BulkAmendmentService service = new BulkAmendmentService(updates, mock(IdentityAuthorization.class));
    private final CatalogCommandContext context = new CatalogCommandContext(
        new IdentityActor(UUID.randomUUID(), Set.of("STAFF")), UUID.randomUUID());
    private final ProductChange change = new ProductChange("Name", null, null, UUID.randomUUID(), Map.of());

    @Test
    void aFailedItemReportsItsOwnCodeAndDoesNotStopTheOthers() {
        UUID missing = UUID.randomUUID();
        UUID broken = UUID.randomUUID();
        UUID fine = UUID.randomUUID();
        when(updates.update(eq(context), eq(missing), any()))
            .thenThrow(new DomainException(GenErrorCode.NOT_FOUND, "Product not found."));
        when(updates.update(eq(context), eq(broken), any())).thenThrow(new IllegalStateException("boom"));

        List<BulkAmendmentOutcome> outcomes = service.amend(context, List.of(new BulkAmendment(missing, change),
            new BulkAmendment(broken, change), new BulkAmendment(fine, change)));

        assertThat(outcomes).hasSize(3);
        assertThat(outcomes.get(0)).isEqualTo(BulkAmendmentOutcome.failed(missing, "ECP-GEN-4040", "Product not found."));
        assertThat(outcomes.get(1).code()).isEqualTo("ECP-GEN-5000");
        assertThat(outcomes.get(1).detail()).doesNotContain("boom");
        assertThat(outcomes.get(2)).isEqualTo(BulkAmendmentOutcome.applied(fine));
    }
}
