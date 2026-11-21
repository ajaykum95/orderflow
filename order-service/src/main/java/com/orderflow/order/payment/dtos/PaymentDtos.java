package com.orderflow.order.payment.dtos;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDtos {

    public enum PaymentStatus { SUCCESS, DECLINED, REFUNDED }

    public record ChargeCommand(Long orderId, BigDecimal amount, String currency, String idempotencyKey) {}

    public record PaymentResult(String paymentId, Long orderId, BigDecimal amount, String currency,
                                PaymentStatus status, Instant createdAt) {}

    public record GatewayChargeBody(Long orderId, BigDecimal amount, String currency) {}
}
