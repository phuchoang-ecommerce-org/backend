package org.phuchoang.ecp.catalog.application.command;

import org.phuchoang.ecp.catalog.application.port.CatalogCategoryWriter;
import org.phuchoang.ecp.sharedkernel.api.event.EventMetadata;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxEvent;
import org.phuchoang.ecp.sharedkernel.api.event.OutboxWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.UUID;

/** Writes a category and its {@code CategoryChanged} fact within one local transaction. */
@Service
public class CreateCategoryService {

    static final String TOPIC = "ecp.catalog.category.v1";
    private final CatalogCategoryWriter categories;
    private final OutboxWriter outbox;
    private final Clock clock;

    public CreateCategoryService(CatalogCategoryWriter categories, OutboxWriter outbox, Clock clock) {
        this.categories = categories;
        this.outbox = outbox;
        this.clock = clock;
    }

    @Transactional
    public UUID create(CreateCategoryCommand command) {
        CatalogCategoryWriter.CreatedCategory category = categories.create(command);
        Instant occurredAt = clock.instant();
        String parentId = command.parentId() == null ? "null" : "\"" + command.parentId() + "\"";
        String payload = "{\"id\":\"" + command.id() + "\",\"parentId\":" + parentId
            + ",\"name\":\"" + json(command.name()) + "\",\"slug\":\"" + json(command.slug())
            + "\",\"path\":\"" + json(category.path()) + "\",\"depth\":" + category.depth()
            + ",\"sortOrder\":" + command.sortOrder() + "}";
        outbox.append(new OutboxEvent(new EventMetadata(UUID.randomUUID(), "CategoryChanged", 1, occurredAt,
            "Category", command.id(), command.correlationId(), command.actor()), TOPIC, payload));
        return command.id();
    }

    private static String json(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
            .replace("\b", "\\b").replace("\f", "\\f").replace("\n", "\\n")
            .replace("\r", "\\r").replace("\t", "\\t");
    }
}
