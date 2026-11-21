package com.orderflow.order.order.entities;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
    name = "orders",
    uniqueConstraints = @UniqueConstraint(columnNames = {"customer_email", "idempotency_key"})
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_email", nullable = false)
    private String customerEmail;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus status = OrderStatus.PENDING;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    protected Order() {}

    public Order(String customerEmail, String idempotencyKey) {
        this.customerEmail = customerEmail;
        this.idempotencyKey = idempotencyKey;
    }

    public void addItem(OrderItem item) {
        item.setOrder(this);
        items.add(item);
        totalAmount = totalAmount.add(item.lineTotal());
    }

    public void moveTo(OrderStatus next) {
        if (!status.canMoveTo(next)) {
            throw new IllegalStateException("Order " + id + ": " + status + " -> " + next + " not allowed");
        }
        this.status = next;
    }

    public Long getId() { return id; }
    public String getCustomerEmail() { return customerEmail; }
    public String getIdempotencyKey() { return idempotencyKey; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public Instant getCreatedAt() { return createdAt; }
    public List<OrderItem> getItems() { return items; }
}
