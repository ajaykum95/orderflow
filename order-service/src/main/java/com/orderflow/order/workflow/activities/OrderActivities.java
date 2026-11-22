package com.orderflow.order.workflow.activities;

import io.temporal.activity.ActivityInterface;

@ActivityInterface
public interface OrderActivities {

    void reserveStock(Long orderId);

    // payment का id लौटाता है (refund के लिए चाहिए)
    String chargePayment(Long orderId);

    void generateInvoice(Long orderId);

    void sendNotification(Long orderId);               // "order confirmed" email

    void sendCancellationNotification(Long orderId);   // नया: "order रद्द हुआ" email

    void completeOrder(Long orderId);
}