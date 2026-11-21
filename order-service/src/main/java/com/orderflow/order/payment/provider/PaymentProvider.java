package com.orderflow.order.payment.provider;

import com.orderflow.order.payment.dtos.PaymentDtos.ChargeCommand;
import com.orderflow.order.payment.dtos.PaymentDtos.PaymentResult;

public interface PaymentProvider {

    PaymentResult charge(ChargeCommand command);

    PaymentResult refund(String paymentId);
}
