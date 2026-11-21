package com.orderflow.order.order.entities;

public enum OrderStatus {
    PENDING,        // Order created, no actions processed yet
    STOCK_RESERVED, // Inventory/stock has been reserved
    PAID,           // Payment has been completed
    COMPLETED,      // Invoice generated and confirmation email sent
    CANCELLED,      // Order cancelled (after rollback)
    FAILED;         // Failed at some step during processing

    public boolean canMoveTo(OrderStatus next) {
        return switch (this) {
            case PENDING        -> next == STOCK_RESERVED || next == CANCELLED || next == FAILED;
            case STOCK_RESERVED -> next == PAID || next == CANCELLED || next == FAILED;
            case PAID           -> next == COMPLETED || next == CANCELLED;
            default             -> false;   // Nothing after COMPLETED / CANCELLED / FAILED
        };
    }
}
