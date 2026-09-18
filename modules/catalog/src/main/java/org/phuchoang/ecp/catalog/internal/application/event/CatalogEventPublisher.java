package org.phuchoang.ecp.catalog.internal.application.event;

import org.phuchoang.ecp.catalog.internal.application.command.CatalogCommandContext;
import org.phuchoang.ecp.catalog.internal.domain.event.CatalogDomainEvent;
import org.phuchoang.ecp.catalog.internal.domain.event.CategoryChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductCreated;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductDiscontinued;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPriceChanged;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductPublished;
import org.phuchoang.ecp.catalog.internal.domain.event.ProductUpdated;
import org.phuchoang.ecp.catalog.internal.domain.event.VariantAdded;
import org.phuchoang.ecp.catalog.internal.domain.repository.CategoryRepository;
import org.phuchoang.ecp.sharedkernel.api.event.EventMetadata;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxEvent;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxWriter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Clock;
import java.util.UUID;

/**
 * The single path by which a Catalog business fact leaves the module: it is appended to
 * {@code catalog_outbox} in the command's own transaction (`ADR-0011`), never sent to Kafka
 * directly. This class only coordinates — routing, metadata, and the affected-category lookup;
 * {@link CatalogEventPayloadMapper} owns the wire shape.
 */
@Component
public class CatalogEventPublisher {

    static final String PRODUCT_TOPIC = "ecp.catalog.product.v1";
    static final String CATEGORY_TOPIC = "ecp.catalog.category.v1";
    private static final int EVENT_VERSION = 1;

    private final OutboxWriter outbox;
    private final CategoryRepository categories;
    private final CatalogEventPayloadMapper payloads;
    private final ObjectMapper json;
    private final Clock clock;

    public CatalogEventPublisher(OutboxWriter outbox, CategoryRepository categories, CatalogEventPayloadMapper payloads,
            ObjectMapper json, Clock clock) {
        this.outbox = outbox;
        this.categories = categories;
        this.payloads = payloads;
        this.json = json;
        this.clock = clock;
    }

    public void publish(CatalogDomainEvent event, CatalogCommandContext context) {
        Object payload = payloads.payload(event, affectedCategories(event));
        EventMetadata metadata = new EventMetadata(UUID.randomUUID(), event.eventType(), EVENT_VERSION,
            clock.instant(), event.aggregateType(), event.aggregateId(), context.correlationId(), context.actor());
        outbox.append(new OutboxEvent(metadata, topic(event), serialize(payload)));
    }

    private static String topic(CatalogDomainEvent event) {
        return event instanceof CategoryChanged ? CATEGORY_TOPIC : PRODUCT_TOPIC;
    }

    /** Which listings the event invalidates: the subtree beneath the product's category, or the category itself. */
    private AffectedCategories affectedCategories(CatalogDomainEvent event) {
        UUID categoryId = switch (event) {
            case ProductCreated created -> created.product().categoryId();
            case ProductUpdated updated -> updated.product().categoryId();
            case ProductPublished published -> published.product().categoryId();
            case VariantAdded added -> added.categoryId();
            case CategoryChanged changed -> changed.removed() ? null : changed.category().id();
            case ProductPriceChanged ignored -> null;
            case ProductDiscontinued ignored -> null;
        };
        return categoryId == null ? AffectedCategories.NONE : AffectedCategories.of(categories.findSubtree(categoryId));
    }

    private String serialize(Object payload) {
        try {
            return json.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new IllegalStateException("Cannot serialize catalog event payload", exception);
        }
    }
}
