package com.orderflow.payment.dto;

import com.orderflow.payment.model.Payment;
import com.orderflow.payment.model.PaymentStatus;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

public class PaymentDtos {

    public record ChargeRequest(
            @NotNull Long orderId,
            @NotNull @DecimalMin("0.01") BigDecimal amount,
            @NotBlank String currency
    ) {}

    public record PaymentResponse(
            String paymentId, Long orderId, BigDecimal amount, String currency,
            PaymentStatus status, Instant createdAt
    ) {
        public static PaymentResponse from(Payment p) {
            return new PaymentResponse(p.getId(), p.getOrderId(), p.getAmount(),
                    p.getCurrency(), p.getStatus(), p.getCreatedAt());
        }
    }

    public record ChaosRequest(
            @DecimalMin("0.0") @DecimalMax("1.0") Double failureRate,
            @DecimalMin("0.0") @DecimalMax("1.0") Double slowRate,
            @Min(0) @Max(30000) Long slowDelayMs
    ) {}
}
