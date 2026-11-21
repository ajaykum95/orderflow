package com.orderflow.order.workflow.activities.impl;

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

@Component
@ActivityImpl(taskQueues = WorkflowConstants.TASK_QUEUE)
public class CompensationActivitiesImpl implements CompensationActivities {

    private static final Logger log = LoggerFactory.getLogger(CompensationActivitiesImpl.class);

    private final OrderFulfillmentService fulfillment;
    private final PaymentProvider paymentProvider;

    public CompensationActivitiesImpl(OrderFulfillmentService fulfillment, PaymentProvider paymentProvider) {
        this.fulfillment = fulfillment;
        this.paymentProvider = paymentProvider;
    }

    @Override
    public void releaseStockAndCancel(Long orderId) {
        log.warn("[order {}] ROLLBACK: stock वापस और order रद्द", orderId);
        fulfillment.releaseAndCancel(orderId);
    }

    @Override
    public void refundPayment(String paymentId) {
        log.warn("ROLLBACK: payment {} का refund", paymentId);
        try {
            paymentProvider.refund(paymentId);
        } catch (ResponseStatusException e) {
            // 4xx: दोबारा कोशिश से कुछ नहीं बदलेगा; साफ़ आवाज़ में फेल होने दो ताकि कोई इंसान देखे
            throw ApplicationFailure.newNonRetryableFailure("Refund rejected: " + e.getReason(), "REFUND_REJECTED");
        }
        // 5xx / timeout / circuit open -> सामान्य exception -> Temporal retry करेगा
    }

    @Override
    public void markFailedIfPending(Long orderId) {
        fulfillment.markFailedIfPending(orderId);
    }
}
