package com.orders.dto;

import java.math.BigDecimal;
import java.time.Instant;

public class OrderDocument {

    public Long orderId;
    public String customerId;
    public BigDecimal amountUSD;
    public String targetCurrency;
    public BigDecimal convertedAmount;
    public String status;
    public Instant createdAt;

    public OrderDocument() {
    }
}
