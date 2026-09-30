package org.phuchoang.ecp.messaging.search;

import org.apache.kafka.common.TopicPartition;
import org.springframework.boot.kafka.autoconfigure.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.ExponentialBackOff;

/** Retry transient Elasticsearch failures without stalling a partition forever on a poison event. */
@Configuration(proxyBeanMethods = false)
class CatalogSearchKafkaConfiguration {

    private static final String DLT_SUFFIX = ".dlt.catalog-search";

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> catalogSearchKafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory, KafkaTemplate<Object, Object> kafkaTemplate) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory = new ConcurrentKafkaListenerContainerFactory<>();
        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(errorHandler(kafkaTemplate));
        return factory;
    }

    static DefaultErrorHandler errorHandler(KafkaTemplate<Object, Object> kafkaTemplate) {
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(kafkaTemplate,
            (record, exception) -> new TopicPartition(record.topic() + DLT_SUFFIX, record.partition()));
        recoverer.setFailIfSendResultIsError(true);

        ExponentialBackOff retryBackOff = new ExponentialBackOff(1_000L, 2.0d);
        retryBackOff.setMaxInterval(4_000L);
        retryBackOff.setMaxAttempts(3);
        return new DefaultErrorHandler(recoverer, retryBackOff);
    }
}
