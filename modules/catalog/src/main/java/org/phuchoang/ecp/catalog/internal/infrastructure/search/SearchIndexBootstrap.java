package org.phuchoang.ecp.catalog.internal.infrastructure.search;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;

/**
 * Creates the versioned index from its checked-in mapping before any projection event can write.
 * A mapping change is a new version plus alias swap, never Elasticsearch dynamic mapping.
 */
@Component
class SearchIndexBootstrap {

    private static final Logger log = LoggerFactory.getLogger(SearchIndexBootstrap.class);

    private final ElasticsearchClient client;

    SearchIndexBootstrap(ElasticsearchClient client) {
        this.client = client;
    }

    @EventListener(ApplicationReadyEvent.class)
    void initialize() {
        try {
            if (!client.indices().exists(request -> request.index(SearchIndex.VERSIONED_NAME)).value()) {
                ClassPathResource mapping = new ClassPathResource(SearchIndex.MAPPING_RESOURCE);
                try (Reader source = new InputStreamReader(mapping.getInputStream(), StandardCharsets.UTF_8)) {
                    client.indices().create(request -> request.index(SearchIndex.VERSIONED_NAME).withJson(source));
                }
            }
            if (!client.indices().existsAlias(request -> request.name(SearchIndex.ALIAS)).value()) {
                client.indices().updateAliases(request -> request.actions(action -> action.add(add -> add
                    .index(SearchIndex.VERSIONED_NAME).alias(SearchIndex.ALIAS).isWriteIndex(true))));
            }
        } catch (Exception exception) {
            // Catalog remains available when the optional read model is temporarily unavailable.
            log.warn("Product search index bootstrap deferred; search requests will return a retryable error until Elasticsearch recovers.",
                exception);
        }
    }
}
