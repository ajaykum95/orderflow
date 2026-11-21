package com.orderflow.order.payment.controllers;

import com.orderflow.order.common.ApiResponse;
import com.orderflow.order.payment.dtos.PaymentDtos.ChargeCommand;
import com.orderflow.order.payment.dtos.PaymentDtos.PaymentResult;
import com.orderflow.order.payment.provider.PaymentProvider;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

// ⚠️ सिर्फ़ जाँच के लिए (सिर्फ़ ADMIN)। Saga (Module 5) बनने के बाद इसे हटा देंगे।
@RestController
@RequestMapping("/api/admin/payments")
public class PaymentTestController {

    public record TestChargeRequest(@NotNull Long orderId, @NotNull @DecimalMin("0.01") BigDecimal amount) {}

    private final PaymentProvider provider;
    private final CircuitBreakerRegistry registry;

    public PaymentTestController(PaymentProvider provider, CircuitBreakerRegistry registry) {
        this.provider = provider;
        this.registry = registry;
    }

    @PostMapping("/test-charge")
    public ResponseEntity<ApiResponse<PaymentResult>> charge(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @jakarta.validation.Valid @RequestBody TestChargeRequest req) {
        String idKey = (key == null || key.isBlank()) ? UUID.randomUUID().toString() : key;
        PaymentResult result = provider.charge(new ChargeCommand(req.orderId(), req.amount(), "INR", idKey));
        return ResponseEntity.ok(ApiResponse.ok(200, "Charge result: " + result.status(), result));
    }

    @PostMapping("/test-refund/{paymentId}")
    public ResponseEntity<ApiResponse<PaymentResult>> refund(@PathVariable String paymentId) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Refund done", provider.refund(paymentId)));
    }

    @GetMapping("/circuit")
    public ResponseEntity<ApiResponse<Map<String, Object>>> circuit() {
        CircuitBreaker cb = registry.circuitBreaker("paymentService");
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("state", cb.getState().name());   // CLOSED / OPEN / HALF_OPEN
        info.put("failureRatePercent", cb.getMetrics().getFailureRate());
        info.put("successfulCalls", cb.getMetrics().getNumberOfSuccessfulCalls());
        info.put("failedCalls", cb.getMetrics().getNumberOfFailedCalls());
        info.put("notPermittedCalls", cb.getMetrics().getNumberOfNotPermittedCalls());
        return ResponseEntity.ok(ApiResponse.ok(200, "Circuit breaker status", info));
    }
}
