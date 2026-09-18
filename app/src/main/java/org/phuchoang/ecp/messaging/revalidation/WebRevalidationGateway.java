package org.phuchoang.ecp.messaging.revalidation;

import org.phuchoang.ecp.messaging.EventEnvelope;

/** The external-system boundary to the storefront's revalidation endpoint. */
public interface WebRevalidationGateway {

    /**
     * Forwards the event so the storefront can revalidate its pages. The original body is sent
     * verbatim (the signature covers those exact bytes).
     *
     * @throws IllegalStateException when the callback is unconfigured, rejects the signature, or
     *     answers anything but {@code 204} — the caller's transaction rolls back and Kafka redelivers
     */
    void revalidate(EventEnvelope event, String originalBody);
}
