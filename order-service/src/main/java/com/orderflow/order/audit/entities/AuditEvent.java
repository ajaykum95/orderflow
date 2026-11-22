package com.orderflow.order.audit.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;

// MongoDB में हर घटना एक document है। इसमें कोई setter नहीं: एक बार लिखा तो बदलता नहीं।
//
// _id हम खुद बनाते हैं:  "order-5:PAYMENT_SUCCEEDED"
// इसलिए एक order की एक घटना सिर्फ़ एक बार दर्ज हो सकती है। Temporal activity दोबारा चले,
// तो दूसरी बार लिखने पर Mongo खुद मना कर देता है (duplicate key) और हम उसे चुपचाप छोड़ देते हैं।
@Document(collection = "audit_events")
public class AuditEvent {

    @Id
    private String id;

    @Indexed
    private Long orderId;

    private String type;      // AuditEventType का नाम

    private String actor;     // किसने किया: customer का email, या "system:workflow"

    @Indexed
    private Instant timestamp;

    private Map<String, Object> details;   // घटना की अतिरिक्त जानकारी (paymentId, invoiceNumber, कारण...)

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
