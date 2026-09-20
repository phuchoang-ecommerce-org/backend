package org.phuchoang.ecp.messaging.revalidation;

import java.util.UUID;

/**
 * The storefront received the callback but rejected its signature. Retrying
 * cannot repair a trust-boundary mismatch, so the Kafka error policy sends the
 * event directly to the consumer-scoped DLT and raises an operational signal.
 */
final class RevalidationSignatureRejectedException extends IllegalStateException {

    RevalidationSignatureRejectedException(UUID eventId) {
        super("Web revalidation signature rejected for eventId=" + eventId);
    }
}
