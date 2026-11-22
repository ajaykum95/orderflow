package com.orderflow.order.audit.entities;

public enum AuditEventType {
    ORDER_CREATED,
    STOCK_RESERVED,
    PAYMENT_SUCCEEDED,
    INVOICE_ISSUED,
    NOTIFICATION_SENT,
    ORDER_COMPLETED,
    PAYMENT_REFUNDED,
    STOCK_RELEASED,
    ORDER_ROLLED_BACK
}
