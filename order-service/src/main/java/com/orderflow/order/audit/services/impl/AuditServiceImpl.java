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
            // insert (save नहीं): save पुरानी घटना को ऊपर से लिख देता, insert दोहराव पर मना करता है
            repository.insert(new AuditEvent(orderId, type, actor, details));
        } catch (DuplicateKeyException e) {
            log.debug("[order {}] {} पहले से दर्ज है", orderId, type);   // Temporal का दोबारा चलना; सामान्य बात
        } catch (Exception e) {
            // Mongo बंद/धीमा हो तो भी order का काम न रुके; सिर्फ़ आवाज़ करो
            log.error("[order {}] audit घटना {} दर्ज नहीं हो सकी (order पर असर नहीं)", orderId, type, e);
        }
    }

    @Override
    public List<AuditEventResponse> timeline(Long orderId) {
        return repository.findByOrderId(orderId).stream()
                // पहले समय के हिसाब से; एक ही पल हो तो घटनाओं के स्वाभाविक क्रम से
                .sorted(Comparator.comparing(AuditEvent::getTimestamp)
                        .thenComparing(e -> AuditEventType.valueOf(e.getType())))
                .map(AuditEventResponse::from)
                .toList();
    }
}
