package com.orderflow.order.workflow.activities;

import io.temporal.activity.ActivityInterface;

// Activity = workflow का एक असली काम (database / बाहर की service से बात)।
// Workflow का अपना code सिर्फ़ फैसले लेता है; असली काम सब यहाँ होता है।
@ActivityInterface
public interface OrderActivities {

    void reserveStock(Long orderId);

    // payment का id लौटाता है (refund के लिए चाहिए)
    String chargePayment(Long orderId);

    void generateInvoice(Long orderId);     // Module 6 में असली बनेगा

    void sendNotification(Long orderId);    // Module 7 में असली बनेगा

    void completeOrder(Long orderId);
}
