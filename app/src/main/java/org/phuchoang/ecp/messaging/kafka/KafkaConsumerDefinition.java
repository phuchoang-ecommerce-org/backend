package org.phuchoang.ecp.messaging.kafka;

/** A declared subscriber of a source topic; each one owns a dead-letter topic of its own. */
public record KafkaConsumerDefinition(String groupId) {

    private static final String PREFIX = "ecp.";

    public KafkaConsumerDefinition {
        if (!groupId.startsWith(PREFIX)) {
            throw new IllegalArgumentException("Consumer group ids are namespaced under 'ecp.': " + groupId);
        }
    }

    /** The consumer's short name, used as the DLT suffix. */
    public String shortName() {
        return groupId.substring(PREFIX.length());
    }
}
