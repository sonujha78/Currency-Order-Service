package com.orders.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.dto.OrderDocument;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.core.IndexRequest;

@ApplicationScoped
public class OrderIndexerListener {

    private static final Logger LOG = Logger.getLogger(OrderIndexerListener.class);

    @Inject
    OpenSearchClient openSearchClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "opensearch.index")
    String indexName;

    @Incoming("order-created-in")
    public void consume(String payload) {
        try {
            OrderDocument doc = objectMapper.readValue(payload, OrderDocument.class);

            IndexRequest<OrderDocument> request = IndexRequest.of(i -> i
                    .index(indexName)
                    .id(String.valueOf(doc.orderId))
                    .document(doc)
            );

            openSearchClient.index(request);
            LOG.infof("Indexed order id=%d into OpenSearch index=%s", doc.orderId, indexName);
        } catch (Exception e) {
            LOG.error("Failed to index order event into OpenSearch: " + payload, e);
        }
    }
}
