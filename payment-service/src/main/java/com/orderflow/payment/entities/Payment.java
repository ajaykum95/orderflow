package com.orderflow.payment.model;

import java.math.BigDecimal;
import java.time.Instant;

public class Payment {

    private final String id;
    private final Long orderId;
    private final BigDecimal amount;
    private final String currency;
    private PaymentStatus status;
    private final Instant createdAt = Instant.now();

    public Payment(String id, Long orderId, BigDecimal amount, String currency, PaymentStatus status) {
        this.id = id;
        this.orderId = orderId;
        this.amount = amount;
        this.currency = currency;
        this.status = status;
    }

    public String getId() { return id; }
    public Long getOrderId() { return orderId; }
    public BigDecimal getAmount() { return amount; }
    public String getCurrency() { return currency; }
    public synchronized PaymentStatus getStatus() { return status; }
    public synchronized void setStatus(PaymentStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
}
