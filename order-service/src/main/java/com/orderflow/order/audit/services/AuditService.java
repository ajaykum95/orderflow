package com.orderflow.order.audit.services;

import com.orderflow.order.audit.dtos.AuditDtos.AuditEventResponse;
import com.orderflow.order.audit.entities.AuditEventType;

import java.util.List;
import java.util.Map;

public interface AuditService {

    void record(Long orderId, AuditEventType type, String actor, Map<String, Object> details);

    List<AuditEventResponse> timeline(Long orderId);
}
