package com.orderflow.order.workflow.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface CompensationActivities {

    void releaseStockAndCancel(Long orderId);

    void refundPayment(String paymentId);

    void markFailedIfPending(Long orderId);

    void recordRollback(Long orderId, String failedStep, String reason);
}
