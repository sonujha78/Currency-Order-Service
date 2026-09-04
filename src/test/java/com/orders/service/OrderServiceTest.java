package com.orders.service;

import com.orders.client.ExchangeRateClient;
import com.orders.dto.ExchangeRateResponse;
import com.orders.dto.OrderRequestDTO;
import com.orders.dto.OrderResponseDTO;
import com.orders.exception.ExternalApiException;
import com.orders.exception.InvalidCurrencyException;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@QuarkusTest
class OrderServiceTest {

    @Inject
    OrderService orderService;

    @InjectMock
    @RestClient
    ExchangeRateClient exchangeRateClient;

    private ExchangeRateResponse mockRateResponse;

    @BeforeEach
    void setUp() {
        mockRateResponse = new ExchangeRateResponse();
        mockRateResponse.result = "success";
        mockRateResponse.base_code = "USD";

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.9200"));
        rates.put("GBP", new BigDecimal("0.7900"));
        rates.put("JPY", new BigDecimal("150.5000"));
        mockRateResponse.rates = rates;
    }

    @Test
    void createOrder_withValidCurrency_shouldReturnCorrectConvertedAmount() {
        when(exchangeRateClient.getLatestRates("USD")).thenReturn(mockRateResponse);

        OrderRequestDTO request = new OrderRequestDTO();
        request.customerId = "CUST-TEST-001";
        request.amountUSD = new BigDecimal("100.00");
        request.targetCurrency = "EUR";

        OrderResponseDTO response = orderService.createOrder(request);

        assertNotNull(response);
        assertNotNull(response.orderId);
        assertEquals("CUST-TEST-001", response.customerId);
        assertEquals("EUR", response.targetCurrency);
        assertEquals(new BigDecimal("92.00"), response.convertedAmount);
        assertEquals("PROCESSED", response.status);
        assertNotNull(response.createdAt);
    }

    @Test
    void createOrder_withLowercaseCurrency_shouldNormalizeToUppercase() {
        when(exchangeRateClient.getLatestRates("USD")).thenReturn(mockRateResponse);

        OrderRequestDTO request = new OrderRequestDTO();
        request.customerId = "CUST-TEST-002";
        request.amountUSD = new BigDecimal("50.00");
        request.targetCurrency = "gbp";

        OrderResponseDTO response = orderService.createOrder(request);

        assertEquals("GBP", response.targetCurrency);
        assertEquals(new BigDecimal("39.50"), response.convertedAmount);
    }

    @Test
    void createOrder_withInvalidCurrency_shouldThrowInvalidCurrencyException() {
        when(exchangeRateClient.getLatestRates("USD")).thenReturn(mockRateResponse);

        OrderRequestDTO request = new OrderRequestDTO();
        request.customerId = "CUST-TEST-003";
        request.amountUSD = new BigDecimal("100.00");
        request.targetCurrency = "XYZ";

        InvalidCurrencyException exception = assertThrows(
                InvalidCurrencyException.class,
                () -> orderService.createOrder(request)
        );

        assertTrue(exception.getMessage().contains("XYZ"));
    }

    @Test
    void createOrder_whenExternalApiFails_shouldThrowExternalApiException() {
        when(exchangeRateClient.getLatestRates("USD")).thenThrow(new RuntimeException("Connection timeout"));

        OrderRequestDTO request = new OrderRequestDTO();
        request.customerId = "CUST-TEST-004";
        request.amountUSD = new BigDecimal("100.00");
        request.targetCurrency = "EUR";

        assertThrows(ExternalApiException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_whenExternalApiReturnsNullRates_shouldThrowExternalApiException() {
        ExchangeRateResponse emptyResponse = new ExchangeRateResponse();
        emptyResponse.result = "error";
        emptyResponse.rates = null;

        when(exchangeRateClient.getLatestRates("USD")).thenReturn(emptyResponse);

        OrderRequestDTO request = new OrderRequestDTO();
        request.customerId = "CUST-TEST-005";
        request.amountUSD = new BigDecimal("100.00");
        request.targetCurrency = "EUR";

        assertThrows(ExternalApiException.class, () -> orderService.createOrder(request));
    }

    @Test
    void createOrder_shouldRoundConvertedAmountToTwoDecimalPlaces() {
        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("JPY", new BigDecimal("150.567"));
        mockRateResponse.rates = rates;

        when(exchangeRateClient.getLatestRates("USD")).thenReturn(mockRateResponse);

        OrderRequestDTO request = new OrderRequestDTO();
        request.customerId = "CUST-TEST-006";
        request.amountUSD = new BigDecimal("10.00");
        request.targetCurrency = "JPY";

        OrderResponseDTO response = orderService.createOrder(request);

        assertEquals(new BigDecimal("1505.67"), response.convertedAmount);
    }
}
