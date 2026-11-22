package com.orderflow.order.audit.repositories;

import com.orderflow.order.audit.entities.AuditEvent;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuditEventRepository extends MongoRepository<AuditEvent, String> {
    List<AuditEvent> findByOrderId(Long orderId);
}
