package com.orders.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManager;
import org.apache.hc.client5.http.impl.nio.PoolingAsyncClientConnectionManagerBuilder;
import org.apache.hc.core5.http.HttpHost;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.OpenSearchTransport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;

@ApplicationScoped
public class OpenSearchClientProducer {

    @ConfigProperty(name = "opensearch.host")
    String host;

    @ConfigProperty(name = "opensearch.port")
    int port;

    @Inject
    ObjectMapper objectMapper;

    @Produces
    @Dependent
    public OpenSearchClient openSearchClient() {
        PoolingAsyncClientConnectionManager connectionManager =
                PoolingAsyncClientConnectionManagerBuilder.create().build();

        ApacheHttpClient5TransportBuilder builder = ApacheHttpClient5TransportBuilder.builder(
                new HttpHost("http", host, port)
        );
        builder.setHttpClientConfigCallback(httpClientBuilder ->
                httpClientBuilder.setConnectionManager(connectionManager));

        builder.setMapper(new JacksonJsonpMapper(objectMapper));

        OpenSearchTransport transport = builder.build();
        return new OpenSearchClient(transport);
    }
}
