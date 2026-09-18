package org.phuchoang.ecp.messaging;

import org.phuchoang.ecp.messaging.outbox.OutboxRecord;

import java.time.Instant;
import java.util.UUID;

/** Shared fixture: a stored `CategoryChanged` outbox row. */
public final class OutboxRecords {

    public static final UUID EVENT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID CATEGORY_ID = UUID.fromString("22222222-2222-2222-2222-222222222222");
    public static final UUID CORRELATION_ID = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private OutboxRecords() {
    }

    public static OutboxRecord categoryEvent(UUID actorUserId, String actorRole) {
        return new OutboxRecord(1L, EVENT_ID, "CategoryChanged", 1, Instant.parse("2026-09-15T00:00:00Z"),
            "Category", CATEGORY_ID, CORRELATION_ID, actorUserId, actorRole,
            "{\"id\":\"" + CATEGORY_ID + "\",\"parentId\":null,\"name\":\"Shoes\",\"slug\":\"shoes\","
                + "\"path\":\"/" + CATEGORY_ID + "/\",\"depth\":0,\"sortOrder\":0,\"affectedCategorySlugs\":[\"shoes\"],"
                + "\"affectedCategoryIds\":[\"" + CATEGORY_ID + "\"]}",
            "ecp.catalog.category.v1");
    }
}
