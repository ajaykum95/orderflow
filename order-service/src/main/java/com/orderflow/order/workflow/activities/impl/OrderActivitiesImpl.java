package com.orderflow.order.workflow.activities.impl;

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

// दो तरह की गड़बड़ी, और Temporal को दोनों अलग बतानी हैं:
//  - अस्थायी (payment service down, timeout)    -> सामान्य exception        -> Temporal खुद retry करेगा
//  - पक्की (stock नहीं है, payment decline)      -> NonRetryableFailure      -> retry बेकार, सीधा rollback
@Component
@ActivityImpl(taskQueues = WorkflowConstants.TASK_QUEUE)
public class OrderActivitiesImpl implements OrderActivities {

    private static final Logger log = LoggerFactory.getLogger(OrderActivitiesImpl.class);

    private final OrderFulfillmentService fulfillment;
    private final OrderRepository orderRepository;
    private final PaymentProvider paymentProvider;
    private final WorkflowProperties props;

    public OrderActivitiesImpl(OrderFulfillmentService fulfillment,
                               OrderRepository orderRepository,
                               PaymentProvider paymentProvider,
                               WorkflowProperties props) {
        this.fulfillment = fulfillment;
        this.orderRepository = orderRepository;
        this.paymentProvider = paymentProvider;
        this.props = props;
    }

    @Override
    public void reserveStock(Long orderId) {
        log.info("[order {}] stock रोक रहे हैं", orderId);
        try {
            fulfillment.reserveAll(orderId);
        } catch (ResponseStatusException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getReason(), "STOCK_UNAVAILABLE");
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "ORDER_STATE");
        }
    }

    @Override
    public String chargePayment(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> ApplicationFailure.newNonRetryableFailure("Order not found", "ORDER_NOT_FOUND"));

        PaymentResult result;
        try {
            // key हर order की पक्की है; Temporal retry करे तब भी पैसा दोबारा नहीं कटेगा
            result = paymentProvider.charge(new ChargeCommand(
                    orderId, order.getTotalAmount(), "INR", "order-" + orderId + "-charge"));
        } catch (ResponseStatusException e) {
            throw ApplicationFailure.newNonRetryableFailure("Payment rejected: " + e.getReason(), "PAYMENT_REJECTED");
        }
        // PaymentUnavailableException / CallNotPermittedException जानबूझकर नहीं पकड़े -> Temporal retry करेगा

        if (result.status() != PaymentStatus.SUCCESS) {
            throw ApplicationFailure.newNonRetryableFailure("Payment declined", "PAYMENT_DECLINED");
        }

        log.info("[order {}] payment सफल: {}", orderId, result.paymentId());
        try {
            fulfillment.markPaid(orderId);
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "ORDER_STATE");
        }
        return result.paymentId();
    }

    @Override
    public void generateInvoice(Long orderId) {
        // TODO Module 6: असली invoice + PDF
        log.info("[order {}] (placeholder) invoice बना रहे हैं", orderId);
        if (props.isSimulateInvoiceFailure()) {
            throw ApplicationFailure.newNonRetryableFailure("Invoice generation failed (simulated)", "INVOICE_FAILED");
        }
    }

    @Override
    public void sendNotification(Long orderId) {
        // TODO Module 7: असली email (MailHog)
        log.info("[order {}] (placeholder) email भेज रहे हैं", orderId);
    }

    @Override
    public void completeOrder(Long orderId) {
        try {
            fulfillment.complete(orderId);
        } catch (IllegalStateException e) {
            throw ApplicationFailure.newNonRetryableFailure(e.getMessage(), "ORDER_STATE");
        }
        log.info("[order {}] पूरा हुआ", orderId);
    }
}
