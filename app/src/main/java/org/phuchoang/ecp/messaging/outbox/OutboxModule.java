package org.phuchoang.ecp.messaging.outbox;

import java.util.List;

/**
 * The authoritative set of publishing outbox tables. Table names are derived only from these
 * constants — never from configuration or input — which is what makes interpolating them into SQL safe.
 */
public enum OutboxModule {
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

    public String nameValue() {
        return name;
    }

    public String table() {
        return name + "_outbox";
    }

    /** The advisory-lock key that gives each module stream one active publisher across replicas. */
    public String lockKey() {
        return "ecp.outbox." + name;
    }

    public static List<OutboxModule> all() {
        return List.of(values());
    }
}
