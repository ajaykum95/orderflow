package com.orderflow.order.order.entities;

import org.junit.jupiter.api.Test;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.orderflow.order.order.entities.OrderStatus.*;
import static org.assertj.core.api.Assertions.assertThat;

class OrderStatusTest {

    private static final Map<OrderStatus, Set<OrderStatus>> ALLOWED = new EnumMap<>(OrderStatus.class);

    static {
        ALLOWED.put(PENDING, EnumSet.of(STOCK_RESERVED, CANCELLED, FAILED));
        ALLOWED.put(STOCK_RESERVED, EnumSet.of(PAID, CANCELLED, FAILED));
        ALLOWED.put(PAID, EnumSet.of(COMPLETED, CANCELLED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(CANCELLED, EnumSet.noneOf(OrderStatus.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(OrderStatus.class));
    }

    @Test
    void everyTransitionMatchesTheRulebook() {
        for (OrderStatus from : OrderStatus.values()) {
            for (OrderStatus to : OrderStatus.values()) {
                boolean expected = ALLOWED.get(from).contains(to);
                assertThat(from.canMoveTo(to))
                        .as("%s -> %s", from, to)
                        .isEqualTo(expected);
            }
        }
    }

    @Test
    void finishedOrdersCanNeverMoveAgain() {
        for (OrderStatus finished : new OrderStatus[]{COMPLETED, CANCELLED, FAILED}) {
            for (OrderStatus to : OrderStatus.values()) {
                assertThat(finished.canMoveTo(to)).as("%s -> %s", finished, to).isFalse();
            }
        }
    }

    @Test
    void noStateCanMoveToItself() {
        for (OrderStatus s : OrderStatus.values()) {
            assertThat(s.canMoveTo(s)).as(s.name()).isFalse();
        }
    }
}
