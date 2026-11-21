package com.orderflow.order.payment.controllers;

import com.orderflow.order.common.ApiResponse;
import com.orderflow.order.payment.exceptions.PaymentUnavailableException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PaymentExceptionHandler {

    @ExceptionHandler(PaymentUnavailableException.class)
    public ResponseEntity<ApiResponse<Void>> unavailable(PaymentUnavailableException ex) {
        return ResponseEntity.status(503)
                .body(ApiResponse.error(503, "Payment service is temporarily unavailable"));
    }

    @ExceptionHandler(CallNotPermittedException.class)
    public ResponseEntity<ApiResponse<Void>> circuitOpen(CallNotPermittedException ex) {
        return ResponseEntity.status(503)
                .body(ApiResponse.error(503, "Payment circuit breaker is OPEN; please try again shortly"));
    }
}
