package com.orderflow.order.workflow.activities.impl;

import com.orderflow.order.audit.entities.AuditEventType;
import com.orderflow.order.audit.services.AuditService;
import com.orderflow.order.invoice.dtos.InvoiceDtos.InvoiceResponse;
import com.orderflow.order.invoice.services.InvoiceService;
import com.orderflow.order.notification.services.NotificationService;
import com.orderflow.order.order.entities.Order;
import com.orderflow.order.order.repositories.OrderRepository;
import com.orderflow.order.payment.dtos.PaymentDtos.ChargeCommand;
import com.orderflow.order.payment.dtos.PaymentDtos.PaymentResult;
import com.orderflow.order.payment.dtos.PaymentDtos.PaymentStatus;
import com.orderflow.order.payment.provider.PaymentProvider;
import com.orderflow.order.workflow.OrderFulfillmentService;
import com.orderflow.order.workflow.WorkflowConstants;
import com.orderflow.order.workflow.activities.OrderActivities;
import com.orderflow.order.workflow.config.WorkflowProperties;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.ActivityImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@Component
@ActivityImpl(taskQueues = WorkflowConstants.TASK_QUEUE)
public class OrderActivitiesImpl implements OrderActivities {

    private static final Logger log = LoggerFactory.getLogger(OrderActivitiesImpl.class);
    private static final String SYSTEM = "system:workflow";

    private final OrderFulfillmentService fulfillment;
    private final OrderRepository orderRepository;
    private final PaymentProvider paymentProvider;
    private final InvoiceService invoiceService;
    private final NotificationService notificationService;
    private final AuditService audit;
    private final WorkflowProperties props;

    public OrderActivitiesImpl(
            OrderFulfillmentService fulfillment, OrderRepository orderRepository,
            PaymentProvider paymentProvider, InvoiceService invoiceService,
            NotificationService notificationService, AuditService audit,
            WorkflowProperties props) {
        this.fulfillment = fulfillment;
        this.orderRepository = orderRepository;
        this.paymentProvider = paymentProvider;
        this.invoiceService = invoiceService;
        this.notificationService = notificationService;
        this.audit = audit;
        this.props = props;
    }

    @Override
    public void reserveStock(Long orderId) {
        log.info("[order {}] stock stopping", orderId);
        try {
            fulfillment.reserveAll(orderId);
        } catch (ResponseStatusException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getReason(), "STOCK_UNAVAILABLE");
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "ORDER_STATE");
        }
        audit.record(orderId, AuditEventType.STOCK_RESERVED, SYSTEM, Map.of());
    }

    @Override
    public String chargePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApplicationFailure.newNonRetryableFailure("Order not found", "ORDER_NOT_FOUND"));

        PaymentResult result;
        try {
            result = paymentProvider.charge(new ChargeCommand(
                    orderId, order.getTotalAmount(), "INR", "order-" + orderId + "-charge"));
        } catch (ResponseStatusException e) {
            throw ApplicationFailure.newNonRetryableFailure("Payment rejected: " + e.getReason(), "PAYMENT_REJECTED");
        }

        if (result.status() != PaymentStatus.SUCCESS) {
            throw ApplicationFailure.newNonRetryableFailure("Payment declined", "PAYMENT_DECLINED");
        }

        log.info("[order {}] payment success: {}", orderId, result.paymentId());
        try {
            fulfillment.markPaid(orderId);
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "ORDER_STATE");
        }
        audit.record(orderId, AuditEventType.PAYMENT_SUCCEEDED, SYSTEM, Map.of(
                "paymentId", result.paymentId(),
                "amount", order.getTotalAmount().toPlainString()));
        return result.paymentId();
    }

    @Override
    public void generateInvoice(Long orderId) {
        if (props.isSimulateInvoiceFailure()) {
            throw ApplicationFailure.newNonRetryableFailure("Invoice generation failed (simulated)", "INVOICE_FAILED");
        }
        try {
            InvoiceResponse invoice = invoiceService.generateForOrder(orderId);
            log.info("[order {}] invoice created: {}", orderId, invoice.invoiceNumber());
            audit.record(orderId, AuditEventType.INVOICE_ISSUED, SYSTEM,
                    Map.of("invoiceNumber", invoice.invoiceNumber()));
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "INVOICE_STATE");
        }
    }

    @Override
    public void sendNotification(Long orderId) {
        try {
            notificationService.sendOrderConfirmation(orderId);
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "NOTIFICATION_STATE");
        }
        audit.record(orderId, AuditEventType.NOTIFICATION_SENT, SYSTEM, Map.of("type", "ORDER_CONFIRMED"));
    }

    @Override
    public void sendCancellationNotification(Long orderId) {
        try {
            notificationService.sendOrderCancellation(orderId);
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "NOTIFICATION_STATE");
        }
    }

    @Override
    public void completeOrder(Long orderId) {
        try {
            fulfillment.complete(orderId);
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "ORDER_STATE");
        }
        log.info("[order {}] completed", orderId);
        audit.record(orderId, AuditEventType.ORDER_COMPLETED, SYSTEM, Map.of());
    }
}