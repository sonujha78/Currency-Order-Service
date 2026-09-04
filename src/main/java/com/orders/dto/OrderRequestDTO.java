package com.orders.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public class OrderRequestDTO {

    @NotBlank(message = "customerId is required")
    public String customerId;

    @NotNull(message = "amountUSD is required")
    @DecimalMin(value = "0.01", message = "amountUSD must be greater than 0")
    public BigDecimal amountUSD;

    @NotBlank(message = "targetCurrency is required")
    public String targetCurrency;
}
