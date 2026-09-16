package org.phuchoang.ecp.sharedkernel.api.event;

import java.util.UUID;

/** Attribution copied into a durable event envelope; absent for automated work. */
public record EventActor(UUID userId, String role) {

    public EventActor {
        if (userId == null && role != null) {
            throw new IllegalArgumentException("An event actor role requires a user id.");
        }
        if (role != null && role.isBlank()) {
            throw new IllegalArgumentException("An event actor role must not be blank.");
        }
    }
}
