package com.orderflow.order.workflow.activities.impl;

import com.orderflow.order.audit.entities.AuditEventType;
import com.orderflow.order.audit.services.AuditService;
import com.orderflow.order.order.entities.OrderStatus;
import com.orderflow.order.order.repositories.OrderRepository;
import com.orderflow.order.payment.dtos.PaymentDtos.PaymentResult;
import com.orderflow.order.payment.provider.PaymentProvider;
import com.orderflow.order.workflow.OrderFulfillmentService;
import com.orderflow.order.workflow.WorkflowConstants;
import com.orderflow.order.workflow.activities.CompensationActivities;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
@ActivityImpl(taskQueues = WorkflowConstants.TASK_QUEUE)
public class CompensationActivitiesImpl implements CompensationActivities {

    private static final Logger log = LoggerFactory.getLogger(CompensationActivitiesImpl.class);
    private static final String SYSTEM = "system:workflow";

    private final OrderFulfillmentService fulfillment;
    private final PaymentProvider paymentProvider;
    private final OrderRepository orderRepository;
    private final AuditService audit;

    public CompensationActivitiesImpl(OrderFulfillmentService fulfillment, PaymentProvider paymentProvider,
                                      OrderRepository orderRepository, AuditService audit) {
        this.fulfillment = fulfillment;
        this.paymentProvider = paymentProvider;
        this.orderRepository = orderRepository;
        this.audit = audit;
    }

    @Override
    public void releaseStockAndCancel(Long orderId) {
        log.warn("[order {}] ROLLBACK: stock वापस और order रद्द", orderId);
        fulfillment.releaseAndCancel(orderId);

        // audit में "stock वापस" तभी लिखो जब सच में कुछ वापस हुआ (order CANCELLED बना);
        // कुछ रोका ही न गया हो (order PENDING/FAILED) तो यह घटना झूठ होगी
        orderRepository.findById(orderId)
                .filter(o -> o.getStatus() == OrderStatus.CANCELLED)
                .ifPresent(o -> audit.record(orderId, AuditEventType.STOCK_RELEASED, SYSTEM, Map.of()));
    }

    @Override
    public void refundPayment(String paymentId) {
        log.warn("ROLLBACK: payment {} का refund", paymentId);
        PaymentResult result;
        try {
            result = paymentProvider.refund(paymentId);
        } catch (ResponseStatusException e) {
            throw ApplicationFailure.newNonRetryableFailure("Refund rejected: " + e.getReason(), "REFUND_REJECTED");
        }
        if (result != null && result.orderId() != null) {
            audit.record(result.orderId(), AuditEventType.PAYMENT_REFUNDED, SYSTEM, Map.of("paymentId", paymentId));
        }
    }

    @Override
    public void markFailedIfPending(Long orderId) {
        fulfillment.markFailedIfPending(orderId);
    }

    @Override
    public void recordRollback(Long orderId, String failedStep, String reason) {
        String finalStatus = orderRepository.findById(orderId)
                .map(o -> o.getStatus().name()).orElse("UNKNOWN");
        audit.record(orderId, AuditEventType.ORDER_ROLLED_BACK, SYSTEM, Map.of(
                "failedStep", String.valueOf(failedStep),
                "reason", String.valueOf(reason),
                "finalStatus", finalStatus));
    }
}