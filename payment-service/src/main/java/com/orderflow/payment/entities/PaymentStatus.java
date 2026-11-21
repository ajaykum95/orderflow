package com.orderflow.payment.model;

public enum PaymentStatus {
    SUCCESS,    // Payment processed / amount deducted
    DECLINED,   // Payment declined (due to business rules, e.g., amount exceeds limit)
    REFUNDED    // Payment refunded / amount returned
}
