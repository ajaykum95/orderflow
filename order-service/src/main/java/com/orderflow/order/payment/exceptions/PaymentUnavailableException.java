package com.orderflow.order.payment.exceptions;

public class PaymentUnavailableException extends RuntimeException {
    public PaymentUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
