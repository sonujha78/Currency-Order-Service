package com.orders.entity;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
public class Order extends PanacheEntity {

    @Column(name = "customer_id", nullable = false)
    public String customerId;

    @Column(name = "amount_usd", nullable = false)
    public BigDecimal amountUSD;

    @Column(name = "target_currency", nullable = false)
    public String targetCurrency;

    @Column(name = "converted_amount", nullable = false)
    public BigDecimal convertedAmount;

    @Column(name = "status", nullable = false)
    public String status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;
}
