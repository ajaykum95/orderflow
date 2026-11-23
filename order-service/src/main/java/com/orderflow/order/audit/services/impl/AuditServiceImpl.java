package com.orderflow.order.audit.services.impl;

import com.orderflow.order.audit.dtos.AuditDtos.AuditEventResponse;
import com.orderflow.order.audit.entities.AuditEvent;
import com.orderflow.order.audit.entities.AuditEventType;
import com.orderflow.order.audit.repositories.AuditEventRepository;
import com.orderflow.order.audit.services.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class AuditServiceImpl implements AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditServiceImpl.class);

    private final AuditEventRepository repository;

    public AuditServiceImpl(AuditEventRepository repository) {
        this.repository = repository;
    }

    @Override
    public void record(Long orderId, AuditEventType type, String actor, Map<String, Object> details) {
        try {
            repository.insert(new AuditEvent(orderId, type, actor, details));
        } catch (DuplicateKeyException e) {
            log.debug("[order {}] {} already registered.", orderId, type);
        } catch (Exception e) {
            log.error("[order {}] Could not record audit event {} (does not affect order)", orderId, type, e);
        }
    }

    @Override
    public List<AuditEventResponse> timeline(Long orderId) {
        return repository.findByOrderId(orderId).stream()
                .sorted(Comparator.comparing(AuditEvent::getTimestamp)
                        .thenComparing(e -> AuditEventType.valueOf(e.getType())))
                .map(AuditEventResponse::from)
                .toList();
    }
}
