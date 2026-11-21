package com.orderflow.order.order.dtos;

import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderItem;
import com.orderflow.order.order.entities.OrderStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public class OrderDtos {

    public record ItemRequest(
            @NotNull Long productId,
            @Min(1) @Max(100) int quantity
    ) {}

    public record CreateOrderRequest(
            @NotEmpty @Size(max = 20) List<@Valid ItemRequest> items
    ) {}

    public record ItemResponse(
            Long productId, String productName, BigDecimal unitPrice, int quantity, BigDecimal lineTotal
    ) {
        static ItemResponse from(OrderItem i) {
            return new ItemResponse(i.getProductId(), i.getProductName(),
                    i.getUnitPrice(), i.getQuantity(), i.lineTotal());
        }
    }

    public record OrderResponse(
            Long id, OrderStatus status, BigDecimal totalAmount, Instant createdAt, List<ItemResponse> items
    ) {
        public static OrderResponse from(Order o) {
            return new OrderResponse(o.getId(), o.getStatus(), o.getTotalAmount(), o.getCreatedAt(),
                    o.getItems().stream().map(ItemResponse::from).toList());
        }
    }

    public record CreateResult(OrderResponse order, boolean created) {}
}
