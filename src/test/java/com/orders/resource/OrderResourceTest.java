package com.orders.resource;

import com.orders.client.ExchangeRateClient;
import com.orders.dto.ExchangeRateResponse;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

@QuarkusTest
class OrderResourceTest {

    @InjectMock
    @RestClient
    ExchangeRateClient exchangeRateClient;

    @BeforeEach
    void setUp() {
        ExchangeRateResponse mockResponse = new ExchangeRateResponse();
        mockResponse.result = "success";
        mockResponse.base_code = "USD";

        Map<String, BigDecimal> rates = new HashMap<>();
        rates.put("EUR", new BigDecimal("0.9200"));
        rates.put("GBP", new BigDecimal("0.7900"));
        mockResponse.rates = rates;

        when(exchangeRateClient.getLatestRates("USD")).thenReturn(mockResponse);
    }

    @Test
    void createOrder_withValidRequest_shouldReturn201AndOrderDetails() {
        String requestBody = """
                {
                    "customerId": "CUST-IT-001",
                    "amountUSD": 150.00,
                    "targetCurrency": "EUR"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/orders")
                .then()
                .statusCode(201)
                .body("orderId", notNullValue())
                .body("customerId", equalTo("CUST-IT-001"))
                .body("targetCurrency", equalTo("EUR"))
                .body("convertedAmount", equalTo(138.0f))
                .body("status", equalTo("PROCESSED"))
                .body("createdAt", notNullValue());
    }

    @Test
    void createOrder_withMissingCustomerId_shouldReturn400() {
        String requestBody = """
                {
                    "amountUSD": 100.00,
                    "targetCurrency": "EUR"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/orders")
                .then()
                .statusCode(400);
    }

    @Test
    void createOrder_withNegativeAmount_shouldReturn400() {
        String requestBody = """
                {
                    "customerId": "CUST-IT-002",
                    "amountUSD": -50.00,
                    "targetCurrency": "EUR"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/orders")
                .then()
                .statusCode(400);
    }

    @Test
    void createOrder_withMissingTargetCurrency_shouldReturn400() {
        String requestBody = """
                {
                    "customerId": "CUST-IT-003",
                    "amountUSD": 100.00
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/orders")
                .then()
                .statusCode(400);
    }

    @Test
    void createOrder_withInvalidCurrency_shouldReturn400WithErrorBody() {
        String requestBody = """
                {
                    "customerId": "CUST-IT-004",
                    "amountUSD": 100.00,
                    "targetCurrency": "ZZZ"
                }
                """;

        given()
                .contentType(ContentType.JSON)
                .body(requestBody)
                .when()
                .post("/api/v1/orders")
                .then()
                .statusCode(400)
                .body("error", equalTo("INVALID_CURRENCY"))
                .body("message", containsString("ZZZ"));
    }
}
