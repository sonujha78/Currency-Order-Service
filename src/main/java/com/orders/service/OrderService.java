package com.orders.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.orders.client.ExchangeRateClient;
import com.orders.dto.ExchangeRateResponse;
import com.orders.dto.OrderRequestDTO;
import com.orders.dto.OrderResponseDTO;
import com.orders.entity.Order;
import com.orders.exception.ExternalApiException;
import com.orders.exception.InvalidCurrencyException;
import com.orders.messaging.OrderCreatedEvent;
import io.smallrye.reactive.messaging.MutinyEmitter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;

@ApplicationScoped
public class OrderService {

    private static final Logger LOG = Logger.getLogger(OrderService.class);

    @Inject
    @RestClient
    ExchangeRateClient exchangeRateClient;

    @Inject
    @Channel("order-created-out")
    MutinyEmitter<String> orderCreatedEmitter;

    @Inject
    ObjectMapper objectMapper;

    @Transactional
    public OrderResponseDTO createOrder(OrderRequestDTO request) {
        ExchangeRateResponse rateResponse;
        try {
            rateResponse = exchangeRateClient.getLatestRates("USD");
        } catch (Exception e) {
            LOG.error("Failed to call exchange rate API", e);
            throw new ExternalApiException("Could not fetch exchange rates", e);
        }

        if (rateResponse == null || rateResponse.rates == null) {
            throw new ExternalApiException("Exchange rate API returned an empty response", null);
        }

        String targetCurrency = request.targetCurrency.toUpperCase();
        BigDecimal rate = rateResponse.rates.get(targetCurrency);

        if (rate == null) {
            throw new InvalidCurrencyException("Unsupported or invalid target currency: " + targetCurrency);
        }

        BigDecimal convertedAmount = request.amountUSD.multiply(rate).setScale(2, RoundingMode.HALF_UP);

        Order order = new Order();
        order.customerId = request.customerId;
        order.amountUSD = request.amountUSD;
        order.targetCurrency = targetCurrency;
        order.convertedAmount = convertedAmount;
        order.status = "PROCESSED";
        order.createdAt = Instant.now();
        order.persist();

        publishOrderCreatedEvent(order);

        return toResponseDTO(order);
    }

    private void publishOrderCreatedEvent(Order order) {
        try {
            OrderCreatedEvent event = new OrderCreatedEvent(
                    order.id, order.customerId, order.amountUSD,
                    order.targetCurrency, order.convertedAmount,
                    order.status, order.createdAt
            );
            String payload = objectMapper.writeValueAsString(event);
            orderCreatedEmitter.sendAndForget(payload);
        } catch (Exception e) {
            LOG.error("Failed to publish ORDER_CREATED event for order id=" + order.id, e);
        }
    }

    private OrderResponseDTO toResponseDTO(Order order) {
        return new OrderResponseDTO(
                order.id, order.customerId, order.amountUSD,
                order.targetCurrency, order.convertedAmount,
                order.status, order.createdAt
        );
    }
}
