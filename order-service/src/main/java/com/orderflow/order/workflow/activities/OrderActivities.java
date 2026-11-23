package com.orderflow.order.workflow.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OrderActivities {

    void reserveStock(Long orderId);

    String chargePayment(Long orderId);

    void generateInvoice(Long orderId);

    void sendNotification(Long orderId);

    void sendCancellationNotification(Long orderId);

    void completeOrder(Long orderId);
}