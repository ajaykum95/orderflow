package com.orderflow.payment.service;

import com.orderflow.payment.chaos.ChaosSettings;
import com.orderflow.payment.dto.PaymentDtos.ChargeRequest;
import com.orderflow.payment.model.Payment;
import com.orderflow.payment.model.PaymentStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PaymentServiceTest {

    private ChaosSettings chaos;
    private PaymentService service;

    @BeforeEach
    void setUp() {
        chaos = new ChaosSettings(0.0, 0.0, 0);
        service = new PaymentService(chaos);
    }

    private static ChargeRequest charge(String amount) {
        return new ChargeRequest(1L, new BigDecimal(amount), "INR");
    }

    @Test
    void normalAmountSucceeds() {
        assertThat(service.charge("k1", charge("500")).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void amountAboveOneLakhIsDeclined() {
        assertThat(service.charge("k1", charge("100000.01")).getStatus()).isEqualTo(PaymentStatus.DECLINED);
        assertThat(service.charge("k2", charge("100000")).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void sameIdempotencyKeyReturnsTheSamePaymentAndNeverChargesTwice() {
        Payment first = service.charge("k1", charge("500"));
        Payment second = service.charge("k1", charge("500"));

        assertThat(second.getId()).isEqualTo(first.getId());
    }

    @Test
    void differentKeysMakeDifferentPayments() {
        assertThat(service.charge("k1", charge("500")).getId())
                .isNotEqualTo(service.charge("k2", charge("500")).getId());
    }

    @Test
    void refundMovesSuccessToRefundedAndIsRepeatable() {
        Payment p = service.charge("k1", charge("500"));

        assertThat(service.refund(p.getId()).getStatus()).isEqualTo(PaymentStatus.REFUNDED);
        assertThat(service.refund(p.getId()).getStatus()).isEqualTo(PaymentStatus.REFUNDED);   // दोबारा भी ठीक
    }

    @Test
    void declinedPaymentCannotBeRefunded() {
        Payment declined = service.charge("k1", charge("200000"));

        assertThatThrownBy(() -> service.refund(declined.getId()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void unknownPaymentIs404() {
        assertThatThrownBy(() -> service.get("pay_nope"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void gatewayOutageIs503AndLeavesNothingBehind() {
        chaos.update(1.0, null, null);

        assertThatThrownBy(() -> service.charge("k1", charge("500")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.SERVICE_UNAVAILABLE));

        chaos.update(0.0, null, null);
        assertThat(service.charge("k1", charge("500")).getStatus()).isEqualTo(PaymentStatus.SUCCESS);
    }
}
