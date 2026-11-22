package com.orderflow.order.notification.repositories;

import com.orderflow.order.notification.entities.NotificationLog;
import com.orderflow.order.notification.entities.NotificationType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, Long> {
    boolean existsByOrderIdAndType(Long orderId, NotificationType type);
}
