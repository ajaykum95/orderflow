package com.orderflow.order.audit.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

@Document(collection = "audit_events")
public class AuditEvent {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    private String type;

    private String actor;

    @Indexed
    private Instant timestamp;

    private Map<String, Object> details;

    protected AuditEvent() {}

    public AuditEvent(Long orderId, AuditEventType type, String actor, Map<String, Object> details) {
        this.id = "order-" + orderId + ":" + type.name();
        this.orderId = orderId;
        this.type = type.name();
        this.actor = actor;
        this.timestamp = Instant.now();
        this.details = details;
    }

    public String getId() { return id; }
    public Long getOrderId() { return orderId; }
    public String getType() { return type; }
    public String getActor() { return actor; }
    public Instant getTimestamp() { return timestamp; }
    public Map<String, Object> getDetails() { return details; }
}
