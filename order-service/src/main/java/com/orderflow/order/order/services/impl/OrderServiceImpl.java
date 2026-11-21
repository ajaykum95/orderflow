package com.orderflow.order.order.services.impl;

import com.orderflow.order.catalog.dtos.CatalogDtos.ProductResponse;
import com.orderflow.order.catalog.services.ProductService;
import com.orderflow.order.order.dtos.OrderDtos.*;
import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderItem;
import com.orderflow.order.order.entities.OrderStatus;
import com.orderflow.order.order.repositories.OrderRepository;
import com.orderflow.order.order.services.OrderService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final ProductService productService;

    public OrderServiceImpl(OrderRepository orderRepository, ProductService productService) {
        this.orderRepository = orderRepository;
        this.productService = productService;
    }

    @Override
    public CreateResult create(String email, String key, CreateOrderRequest request) {

        var existing = orderRepository.findByCustomerEmailAndIdempotencyKey(email, key);
        if (existing.isPresent()) {
            return new CreateResult(OrderResponse.from(existing.get()), false);
        }

        Order order = new Order(email, key);
        for (ItemRequest item : request.items()) {
            ProductResponse p = productService.get(item.productId());
            order.addItem(new OrderItem(p.id(), p.name(), p.price(), item.quantity()));
        }

        try {
            Order saved = orderRepository.save(order);
            return new CreateResult(OrderResponse.from(saved), true);
        } catch (DataIntegrityViolationException e) {
            Order winner = orderRepository.findByCustomerEmailAndIdempotencyKey(email, key)
                    .orElseThrow(() -> e);
            return new CreateResult(OrderResponse.from(winner), false);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse get(Long orderId, String requesterEmail, boolean isAdmin) {
        Order order = orderRepository.findWithItemsById(orderId)
                .filter(o -> isAdmin || o.getCustomerEmail().equals(requesterEmail))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        return OrderResponse.from(order);
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> listMine(String email, int page, int size) {
        return orderRepository
                .findByCustomerEmailOrderByCreatedAtDesc(email, PageRequest.of(page, Math.min(size, 50)))
                .stream().map(OrderResponse::from).toList();
    }

    @Override
    @Transactional
    public void changeStatus(Long orderId, OrderStatus next) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.moveTo(next);
    }
}
