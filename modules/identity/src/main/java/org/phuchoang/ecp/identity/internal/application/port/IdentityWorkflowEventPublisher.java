package org.phuchoang.ecp.identity.internal.application.port;

import org.phuchoang.ecp.identity.internal.application.event.IdentityWorkflowEvent;

/** Transport port for application-owned workflow and notification signals. */
public interface IdentityWorkflowEventPublisher {
    void publish(IdentityWorkflowEvent event);
}
