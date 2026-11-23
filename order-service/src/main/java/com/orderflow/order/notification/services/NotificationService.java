package com.orderflow.order.notification.services;

public interface NotificationService {

    void sendOrderConfirmation(Long orderId);

    void sendOrderCancellation(Long orderId);
}
