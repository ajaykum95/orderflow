package com.orderflow.order.payment.provider;

import com.orderflow.order.payment.dtos.PaymentDtos.*;
import com.orderflow.order.payment.exceptions.PaymentUnavailableException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.server.ResponseStatusException;

import java.util.function.Supplier;

@Component
public class MockPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(MockPaymentProvider.class);

    private final RestClient client;

    public MockPaymentProvider(@Qualifier("paymentRestClient") RestClient client) {
        this.client = client;
    }

    @Override
    @Retry(name = "paymentService")
    @CircuitBreaker(name = "paymentService")
    public PaymentResult charge(ChargeCommand cmd) {
        log.info("Sending charge to payment-service: order={}, key={}", cmd.orderId(), cmd.idempotencyKey());
        return call(() -> client.post()
                .uri("/api/payments")
                .header("Idempotency-Key", cmd.idempotencyKey())
                .body(new GatewayChargeBody(cmd.orderId(), cmd.amount(), cmd.currency()))
                .retrieve()
                .body(PaymentResult.class));
    }

    @Override
    @Retry(name = "paymentService")
    @CircuitBreaker(name = "paymentService")
    public PaymentResult refund(String paymentId) {
        log.info("Sending refund to payment-serviceं: payment={}", paymentId);
        return call(() -> client.post()
                .uri("/api/payments/{id}/refund", paymentId)
                .retrieve()
                .body(PaymentResult.class));
    }

    private PaymentResult call(Supplier<PaymentResult> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException e) {
            throw new ResponseStatusException(e.getStatusCode(), "Payment service rejected the request");
        } catch (RestClientException e) {
            throw new PaymentUnavailableException("Payment service failed: " + e.getMessage(), e);
        }
    }
}