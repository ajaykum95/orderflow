package com.orderflow.order.audit.dtos;

import com.orderflow.order.audit.entities.AuditEvent;

import java.time.Instant;
import java.util.Map;

public class AuditDtos {

    public record AuditEventResponse(String type, String actor, Instant timestamp, Map<String, Object> details) {
        public static AuditEventResponse from(AuditEvent e) {
            return new AuditEventResponse(e.getType(), e.getActor(), e.getTimestamp(), e.getDetails());
        }
    }
}
