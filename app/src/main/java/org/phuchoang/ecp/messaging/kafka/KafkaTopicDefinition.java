package org.phuchoang.ecp.messaging.kafka;

import java.time.Duration;
import java.util.List;

/** One physical topic: its name, partition count, retention and (for source topics) its consumers. */
public record KafkaTopicDefinition(String name, int partitions, Duration retention,
        List<KafkaConsumerDefinition> consumers) {

    public KafkaTopicDefinition {
        consumers = List.copyOf(consumers);
    }

    /** The consumer-scoped dead-letter topic for one of this topic's subscribers. */
    public KafkaTopicDefinition deadLetterTopicFor(KafkaConsumerDefinition consumer, Duration dltRetention) {
        return new KafkaTopicDefinition(name + ".dlt." + consumer.shortName(), partitions, dltRetention, List.of());
    }
}
