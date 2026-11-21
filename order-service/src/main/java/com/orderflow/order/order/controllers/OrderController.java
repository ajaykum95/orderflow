package com.orderflow.order.order.controllers;

import com.orderflow.order.common.ApiResponse;
import com.orderflow.order.order.dtos.OrderDtos;
import com.orderflow.order.order.services.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<OrderDtos.OrderResponse>> create(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody OrderDtos.CreateOrderRequest request,
            Authentication auth) {

        if (idempotencyKey.isBlank() || idempotencyKey.length() > 100) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(400, "Idempotency-Key must be 1-100 characters"));
        }

        OrderDtos.CreateResult result = orderService.create(auth.getName(), idempotencyKey, request);

        return result.created()
                ? ResponseEntity.status(201).body(ApiResponse.ok(201, "Order created", result.order()))
                : ResponseEntity.ok(ApiResponse.ok(200, "Order already exists for this Idempotency-Key", result.order()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderDtos.OrderResponse>> get(@PathVariable Long id, Authentication auth) {
        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.ok(ApiResponse.ok(200, "Order fetched",
                orderService.get(id, auth.getName(), isAdmin)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<OrderDtos.OrderResponse>>> mine(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Authentication auth) {
        return ResponseEntity.ok(ApiResponse.ok(200, "Orders fetched",
                orderService.listMine(auth.getName(), page, size)));
    }
}
