package com.orderflow.payment.service;

import com.orderflow.payment.chaos.ChaosSettings;
import com.orderflow.payment.dto.PaymentDtos.ChargeRequest;
import com.orderflow.payment.model.Payment;
import com.orderflow.payment.model.PaymentStatus;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class PaymentService {

    private static final BigDecimal DECLINE_ABOVE = new BigDecimal("100000");

    private final Map<String, Payment> byId = new ConcurrentHashMap<>();
    private final Map<String, Payment> byKey = new ConcurrentHashMap<>();   // Idempotency-Key -> payment
    private final ChaosSettings chaos;

    public PaymentService(ChaosSettings chaos) {
        this.chaos = chaos;
    }

    public Payment charge(String idempotencyKey, ChargeRequest req) {
        Payment existing = byKey.get(idempotencyKey);
        if (existing != null) return existing;

        chaos.maybeMisbehave();

        return byKey.computeIfAbsent(idempotencyKey, k -> {
            PaymentStatus status = req.amount().compareTo(DECLINE_ABOVE) > 0
                    ? PaymentStatus.DECLINED : PaymentStatus.SUCCESS;
            Payment p = new Payment("pay_" + UUID.randomUUID().toString().substring(0, 8),
                    req.orderId(), req.amount(), req.currency(), status);
            byId.put(p.getId(), p);
            return p;
        });
    }

    public Payment get(String paymentId) {
        Payment p = byId.get(paymentId);
        if (p == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Payment not found");
        return p;
    }

    public Payment refund(String paymentId) {
        Payment p = get(paymentId);
        synchronized (p) {
            switch (p.getStatus()) {
                case REFUNDED -> {}
                case DECLINED -> throw new ResponseStatusException(HttpStatus.CONFLICT,
                        "Cannot refund a declined payment");
                case SUCCESS -> p.setStatus(PaymentStatus.REFUNDED);
            }
        }
        return p;
    }
}
