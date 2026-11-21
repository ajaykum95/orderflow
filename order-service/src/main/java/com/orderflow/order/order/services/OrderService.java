package com.orderflow.order.order.services;

import com.orderflow.order.order.dtos.OrderDtos.CreateOrderRequest;
import com.orderflow.order.order.dtos.OrderDtos.CreateResult;
import com.orderflow.order.order.dtos.OrderDtos.OrderResponse;
import com.orderflow.order.order.entities.OrderStatus;

import java.util.List;

public interface OrderService {

    CreateResult create(String customerEmail, String idempotencyKey, CreateOrderRequest request);

    OrderResponse get(Long orderId, String requesterEmail, boolean isAdmin);

    List<OrderResponse> listMine(String customerEmail, int page, int size);

    void changeStatus(Long orderId, OrderStatus next);
}
