package com.orderflow.order.audit.entities;

// क्रम जानबूझकर वही है जिस क्रम में घटनाएँ होती हैं (timeline में एक ही पल की घटनाएँ इसी क्रम में दिखेंगी)
public enum AuditEventType {
    ORDER_CREATED,
    STOCK_RESERVED,
    PAYMENT_SUCCEEDED,
    INVOICE_ISSUED,
    NOTIFICATION_SENT,
    ORDER_COMPLETED,
    PAYMENT_REFUNDED,     // rollback: पैसा लौटाया
    STOCK_RELEASED,       // rollback: stock वापस
    ORDER_ROLLED_BACK     // rollback पूरा; कारण और अंतिम हालत के साथ
}
