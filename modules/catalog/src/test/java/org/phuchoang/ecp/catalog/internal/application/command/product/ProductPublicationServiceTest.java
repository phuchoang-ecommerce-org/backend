package org.phuchoang.ecp.catalog.internal.application.command.product;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogPermissions;
import org.phuchoang.ecp.catalog.internal.application.event.CatalogEventPublisher;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPublished;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.PublicationStatus;
import org.phuchoang.ecp.catalog.internal.domain.repository.ProductRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.identity.api.authorization.IdentityAuthorization;
import org.phuchoang.ecp.sharedkernel.api.error.DomainException;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
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

class ProductPublicationServiceTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC);
    private final ProductRepository products = mock(ProductRepository.class);
    private final IdentityAuthorization authorization = mock(IdentityAuthorization.class);
    private final CatalogEventPublisher events = mock(CatalogEventPublisher.class);
    private final ProductPublicationService service = new ProductPublicationService(products, authorization, events, clock);
    private final CatalogCommandContext context = new CatalogCommandContext(
        new IdentityActor(UUID.randomUUID(), Set.of("STAFF")), UUID.randomUUID());

    @Test
    void publishingStampsTheFirstPublicationAndEmitsProductPublished() {
        Product draft = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Shirt", null, null, Map.of());
        when(products.findById(draft.id())).thenReturn(Optional.of(draft));
        when(products.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        ProductSnapshot snapshot = service.setPublication(context, draft.id(), new SetPublication("PUBLISHED", "launch"));

        verify(authorization).assertAuthorized(context.caller(), CatalogPermissions.MANAGE_PRODUCTS);
        assertThat(snapshot.publicationStatus()).isEqualTo("PUBLISHED");
        assertThat(snapshot.publishedAt()).isEqualTo(Instant.now(clock).atOffset(ZoneOffset.UTC));
        assertThat(captured()).isInstanceOf(ProductPublished.class);
    }

    @Test
    void discontinuingAnnouncesTheVariantsWithdrawnAndUnpublishingIsAnUpdate() {
        Product published = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Shirt", null, null, Map.of())
            .transitionTo(PublicationStatus.PUBLISHED, Instant.now(clock));
        when(products.findById(published.id())).thenReturn(Optional.of(published));
        when(products.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.setPublication(context, published.id(), new SetPublication("DISCONTINUED", "eol"));
        service.setPublication(context, published.id(), new SetPublication("UNPUBLISHED", "pause"));

        ArgumentCaptor<CatalogDomainEvent> emitted = ArgumentCaptor.forClass(CatalogDomainEvent.class);
        verify(events, org.mockito.Mockito.times(2)).publish(emitted.capture(), eq(context));
        assertThat(emitted.getAllValues().get(0)).isInstanceOf(ProductDiscontinued.class);
        assertThat(emitted.getAllValues().get(1)).isInstanceOf(ProductUpdated.class);
    }

    @Test
    void anUnknownStatusIsAValidationFailureBeforeAnyLoad() {
        assertThatThrownBy(() -> service.setPublication(context, UUID.randomUUID(), new SetPublication("ARCHIVED", null)))
            .isInstanceOf(DomainException.class).hasMessage("Invalid publicationStatus.");
        verify(products, never()).findById(any());
    }

    @Test
    void aMissingProductIsNotFound() {
        UUID productId = UUID.randomUUID();
        when(products.findById(productId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.setPublication(context, productId, new SetPublication("PUBLISHED", null)))
            .isInstanceOf(DomainException.class).hasMessage("Product not found.");
    }

    private CatalogDomainEvent captured() {
        ArgumentCaptor<CatalogDomainEvent> emitted = ArgumentCaptor.forClass(CatalogDomainEvent.class);
        verify(events).publish(emitted.capture(), eq(context));
        return emitted.getValue();
    }
}
