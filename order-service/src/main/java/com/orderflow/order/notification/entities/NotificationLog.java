package com.orderflow.order.notification.entities;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(
    name = "notification_logs",
    uniqueConstraints = @UniqueConstraint(columnNames = {"order_id", "type"})
)
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_id", nullable = false)
    private Long orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String recipient;

    @Column(nullable = false)
    private Instant sentAt = Instant.now();

    protected NotificationLog() {}

    public NotificationLog(Long orderId, NotificationType type, String recipient) {
        this.orderId = orderId;
        this.type = type;
        this.recipient = recipient;
    }

    public Long getId() { return id; }
    public Long getOrderId() { return orderId; }
    public NotificationType getType() { return type; }
    public String getRecipient() { return recipient; }
    public Instant getSentAt() { return sentAt; }
}
