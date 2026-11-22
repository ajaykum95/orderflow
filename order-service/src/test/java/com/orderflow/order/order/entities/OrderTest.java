package com.orderflow.order.order.entities;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    void totalIsSumOfLineTotals() {
        Order order = new Order("a@test.com", "key-1");
        order.addItem(new OrderItem(1L, "Phone", new BigDecimal("500.00"), 2));
        order.addItem(new OrderItem(2L, "Cable", new BigDecimal("250.50"), 1));

        assertThat(order.getTotalAmount()).isEqualByComparingTo("1250.50");
        assertThat(order.getItems()).hasSize(2);
    }

    @Test
    void newOrderStartsPending() {
        assertThat(new Order("a@test.com", "key-1").getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void happyPathWalksThroughEveryStep() {
        Order order = new Order("a@test.com", "key-1");
        order.moveTo(OrderStatus.STOCK_RESERVED);
        order.moveTo(OrderStatus.PAID);
        order.moveTo(OrderStatus.COMPLETED);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
    }

    @Test
    void skippingAStepIsRejected() {
        Order order = new Order("a@test.com", "key-1");
        assertThatThrownBy(() -> order.moveTo(OrderStatus.PAID))
                .isInstanceOf(IllegalStateException.class);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    }

    @Test
    void completedOrderCannotBeCancelled() {
        Order order = new Order("a@test.com", "key-1");
        order.moveTo(OrderStatus.STOCK_RESERVED);
        order.moveTo(OrderStatus.PAID);
        order.moveTo(OrderStatus.COMPLETED);
        assertThatThrownBy(() -> order.moveTo(OrderStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class);
    }
}
