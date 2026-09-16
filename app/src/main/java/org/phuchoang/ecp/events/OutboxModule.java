package org.phuchoang.ecp.events;

import java.util.List;

/** The authoritative set of existing publishing outbox tables. */
enum OutboxModule {
    CATALOG("catalog"),
    INVENTORY("inventory"),
    ORDERING("ordering"),
    PAYMENT("payment"),
    SHIPPING("shipping"),
    PROMOTION("promotion"),
    REVIEW("review");

    private final String name;

    OutboxModule(String name) {
        this.name = name;
    }

    String nameValue() {
        return name;
    }

    String table() {
        return name + "_outbox";
    }

    static List<OutboxModule> all() {
        return List.of(values());
    }
}
