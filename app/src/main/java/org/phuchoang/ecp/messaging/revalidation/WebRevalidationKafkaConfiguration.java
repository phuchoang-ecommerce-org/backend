package org.phuchoang.ecp.messaging.revalidation;

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

/**
 * Consumer-local failure policy for the catalog-to-storefront boundary. It
 * preserves source-partition ordering with bounded blocking retries and sends
 * terminal failures to the web-revalidation DLT already declared by the topic
 * catalogue.
 */
@Configuration(proxyBeanMethods = false)
class WebRevalidationKafkaConfiguration {

    private static final String DLT_SUFFIX = ".dlt.web-revalidation";

    @Bean
    ConcurrentKafkaListenerContainerFactory<Object, Object> webRevalidationKafkaListenerContainerFactory(
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

        DefaultErrorHandler errorHandler = new DefaultErrorHandler(recoverer, retryBackOff);
        errorHandler.addNotRetryableExceptions(RevalidationSignatureRejectedException.class);
        return errorHandler;
    }
}
