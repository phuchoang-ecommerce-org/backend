package org.phuchoang.ecp.catalog.internal.application.command.variant;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.VariantAdded;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.repository.DuplicateSkuException;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AddVariantServiceTest {

    private final ProductRepository products = mock(ProductRepository.class);
    private final CatalogEventPublisher events = mock(CatalogEventPublisher.class);
    private final AddVariantService service = new AddVariantService(products, mock(IdentityAuthorization.class), events);
    private final CatalogCommandContext context = new CatalogCommandContext(
        new IdentityActor(UUID.randomUUID(), Set.of("STAFF")), UUID.randomUUID());
    private final AddVariant command = new AddVariant("MUG-001", "Blue", new BigDecimal("12.50"), "USD",
        Map.of("colour", "blue"), null, true);

    @Test
    void assignsAnIdentifierPersistsAndAnnouncesTheVariantWithItsCategory() {
        Product product = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Mug", null, null, Map.of());
        when(products.findById(product.id())).thenReturn(Optional.of(product));
        when(products.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        VariantSnapshot snapshot = service.add(context, product.id(), command);

        assertThat(snapshot.id()).isNotNull();
        assertThat(snapshot.sku()).isEqualTo("MUG-001");
        ArgumentCaptor<CatalogDomainEvent> emitted = ArgumentCaptor.forClass(CatalogDomainEvent.class);
        verify(events).publish(emitted.capture(), eq(context));
        VariantAdded added = (VariantAdded) emitted.getValue();
        assertThat(added.categoryId()).isEqualTo(product.categoryId());
        assertThat(added.variant().id()).isEqualTo(snapshot.id());
    }

    @Test
    void aDuplicateSkuIsTheCallersValidationFailure_BR_CAT_01() {
        Product product = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Mug", null, null, Map.of());
        when(products.findById(product.id())).thenReturn(Optional.of(product));
        when(products.save(any())).thenThrow(new DuplicateSkuException("MUG-001", new RuntimeException("unique")));

        assertThatThrownBy(() -> service.add(context, product.id(), command))
            .isInstanceOf(DomainException.class)
            .hasMessage("SKU 'MUG-001' is already in use or retired.")
            .satisfies(e -> assertThat(((DomainException) e).errorCode().code()).isEqualTo("ECP-GEN-4000"));
        verify(events, never()).publish(any(), any());
    }
}
