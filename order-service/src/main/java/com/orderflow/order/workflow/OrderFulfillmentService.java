package com.orderflow.order.workflow;

import com.orderflow.order.catalog.services.ProductService;
import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.entities.OrderItem;
import com.orderflow.order.order.entities.OrderStatus;
import com.orderflow.order.order.repositories.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OrderFulfillmentService {

    private final OrderRepository orders;
    private final ProductService products;

    public OrderFulfillmentService(OrderRepository orders, ProductService products) {
        this.orders = orders;
        this.products = products;
    }

    @Transactional
    public void reserveAll(Long orderId) {
        Order o = load(orderId);
        if (o.getStatus() == OrderStatus.STOCK_RESERVED || o.getStatus() == OrderStatus.PAID
                || o.getStatus() == OrderStatus.COMPLETED) {
            return;
        }
        for (OrderItem item : o.getItems()) {
            products.reserveStock(item.getProductId(), item.getQuantity());
        }
        o.moveTo(OrderStatus.STOCK_RESERVED);
    }

    @Transactional
    public void markPaid(Long orderId) {
        Order o = load(orderId);
        if (o.getStatus() == OrderStatus.PAID || o.getStatus() == OrderStatus.COMPLETED) return;
        o.moveTo(OrderStatus.PAID);
    }

    @Transactional
    public void complete(Long orderId) {
        Order o = load(orderId);
        if (o.getStatus() == OrderStatus.COMPLETED) return;
        o.moveTo(OrderStatus.COMPLETED);
    }

    @Transactional
    public void releaseAndCancel(Long orderId) {
        Order o = load(orderId);
        if (o.getStatus() == OrderStatus.STOCK_RESERVED || o.getStatus() == OrderStatus.PAID) {
            for (OrderItem item : o.getItems()) {
                products.releaseStock(item.getProductId(), item.getQuantity());
            }
            o.moveTo(OrderStatus.CANCELLED);
        }
    }

    @Transactional
    public void markFailedIfPending(Long orderId) {
        Order o = load(orderId);
        if (o.getStatus() == OrderStatus.PENDING) {
            o.moveTo(OrderStatus.FAILED);
        }
    }

    private Order load(Long orderId) {
        return orders.findWithItemsById(orderId)
                .orElseThrow(() -> new IllegalStateException("Order not found: " + orderId));
    }
}
