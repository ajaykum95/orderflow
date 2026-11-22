package com.orderflow.order.workflow.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class WorkflowPropertiesTest {

    private static WorkflowProperties with(List<String> steps) {
        WorkflowProperties p = new WorkflowProperties();
        p.setSteps(steps);
        return p;
    }

    @Test
    void defaultStepsAreValid() {
        assertThatCode(() -> new WorkflowProperties().validate()).doesNotThrowAnyException();
    }

    @Test
    void invoiceAndNotifyAreOptional() {
        assertThatCode(() -> with(List.of("RESERVE_STOCK", "PAYMENT")).validate()).doesNotThrowAnyException();
        assertThatCode(() -> with(List.of("RESERVE_STOCK", "PAYMENT", "NOTIFY")).validate()).doesNotThrowAnyException();
    }

    @Test
    void paymentBeforeReserveStockIsRejected() {
        assertThatThrownBy(() -> with(List.of("PAYMENT", "RESERVE_STOCK")).validate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void missingPaymentOrReserveStockIsRejected() {
        assertThatThrownBy(() -> with(List.of("RESERVE_STOCK", "INVOICE")).validate())
                .isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> with(List.of("PAYMENT", "INVOICE")).validate())
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void unknownStepIsRejected() {
        assertThatThrownBy(() -> with(List.of("RESERVE_STOCK", "TELEPORT", "PAYMENT")).validate())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("TELEPORT");
    }

    @Test
    void emptyOrNullStepsAreRejected() {
        assertThatThrownBy(() -> with(List.of()).validate()).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> with(null).validate()).isInstanceOf(IllegalStateException.class);
    }
}
