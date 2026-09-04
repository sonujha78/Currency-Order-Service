package com.orders.client;

import com.orders.dto.ExchangeRateResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "exchange-rate-api")
@Path("/v6/latest")
public interface ExchangeRateClient {

    @GET
    @Path("/{baseCurrency}")
    @Produces(MediaType.APPLICATION_JSON)
    ExchangeRateResponse getLatestRates(@PathParam("baseCurrency") String baseCurrency);
}
