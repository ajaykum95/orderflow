package com.orderflow.order.notification.services;

public interface NotificationService {

    // दोबारा बुलाने पर दूसरी email नहीं जाती
    void sendOrderConfirmation(Long orderId);

    void sendOrderCancellation(Long orderId);
}
