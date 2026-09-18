package org.phuchoang.ecp.catalog.internal.application.event;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductCreated;
import org.phuchoang.ecp.catalog.internal.domain.model.Category;
import org.phuchoang.ecp.catalog.internal.domain.model.Product;
import org.phuchoang.ecp.catalog.internal.domain.model.SubtreeCategory;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.identity.api.authorization.IdentityActor;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxEvent;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxWriter;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Every Catalog fact is routed to its topic and appended to the outbox with the command's metadata. */
class CatalogEventPublisherTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-09-18T10:00:00Z"), ZoneOffset.UTC);
    private final OutboxWriter outbox = mock(OutboxWriter.class);
    private final CategoryRepository categories = mock(CategoryRepository.class);
    private final CatalogEventPublisher publisher = new CatalogEventPublisher(outbox, categories,
        new CatalogEventPayloadMapper(), new ObjectMapper(), clock);

    @Test
    void productFactsGoToTheProductTopicWithTheSubtreeBeneathTheirCategory() {
        Product product = Product.create(UUID.randomUUID(), UUID.randomUUID(), "Shirt", null, null, Map.of());
        UUID correlationId = UUID.randomUUID();
        UUID accountId = UUID.randomUUID();
        when(categories.findSubtree(product.categoryId()))
            .thenReturn(List.of(new SubtreeCategory(product.categoryId(), "shirts")));

        publisher.publish(new ProductCreated(product),
            new CatalogCommandContext(new IdentityActor(accountId, Set.of("STAFF")), correlationId));

        ArgumentCaptor<OutboxEvent> appended = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).append(appended.capture());
        OutboxEvent event = appended.getValue();
        assertThat(event.topic()).isEqualTo("ecp.catalog.product.v1");
        assertThat(event.metadata().eventType()).isEqualTo("ProductCreated");
        assertThat(event.metadata().aggregateId()).isEqualTo(product.id());
        assertThat(event.metadata().correlationId()).isEqualTo(correlationId);
        assertThat(event.metadata().actor().userId()).isEqualTo(accountId);
        assertThat(event.metadata().occurredAt()).isEqualTo(Instant.now(clock));
        assertThat(event.payload()).contains("\"affectedCategorySlugs\":[\"shirts\"]")
            .contains("\"affectedCategoryIds\":[\"" + product.categoryId() + "\"]");
    }

    @Test
    void categoryFactsGoToTheCategoryTopicAndAGuestHasNoActor() {
        Category category = new Category(UUID.randomUUID(), null, "Shirts", "shirts", "/x/", 0, null, 0, false);
        when(categories.findSubtree(category.id())).thenReturn(List.of(new SubtreeCategory(category.id(), "shirts")));

        publisher.publish(CategoryChanged.of(category),
            new CatalogCommandContext(IdentityActor.GUEST, UUID.randomUUID()));

        ArgumentCaptor<OutboxEvent> appended = ArgumentCaptor.forClass(OutboxEvent.class);
        verify(outbox).append(appended.capture());
        assertThat(appended.getValue().topic()).isEqualTo("ecp.catalog.category.v1");
        assertThat(appended.getValue().metadata().actor()).isNull();
        assertThat(appended.getValue().payload()).contains("\"slug\":\"shirts\"");
    }
}
