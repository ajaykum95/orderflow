package com.orderflow.payment.controller;

import com.orderflow.payment.dto.PaymentDtos.ChargeRequest;
import com.orderflow.payment.dto.PaymentDtos.PaymentResponse;
import com.orderflow.payment.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService service;

    public PaymentController(PaymentService service) {
        this.service = service;
    }

    @PostMapping
    public PaymentResponse charge(@RequestHeader("Idempotency-Key") String idempotencyKey,
                                  @Valid @RequestBody ChargeRequest req) {
        return PaymentResponse.from(service.charge(idempotencyKey, req));
    }

    @GetMapping("/{paymentId}")
    public PaymentResponse get(@PathVariable String paymentId) {
        return PaymentResponse.from(service.get(paymentId));
    }

    @PostMapping("/{paymentId}/refund")
    public PaymentResponse refund(@PathVariable String paymentId) {
        return PaymentResponse.from(service.refund(paymentId));
    }
}
