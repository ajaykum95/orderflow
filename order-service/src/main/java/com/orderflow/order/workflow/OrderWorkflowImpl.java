package com.orderflow.order.workflow;

import com.orderflow.order.workflow.activities.CompensationActivities;
import com.orderflow.order.workflow.activities.OrderActivities;
import io.temporal.activity.ActivityOptions;
import io.temporal.common.RetryOptions;
import io.temporal.failure.ActivityFailure;
import io.temporal.failure.ApplicationFailure;
import io.temporal.spring.boot.WorkflowImpl;
import io.temporal.workflow.Saga;
import io.temporal.workflow.Workflow;
import org.slf4j.Logger;

import java.time.Duration;
import java.util.List;

@WorkflowImpl(taskQueues = WorkflowConstants.TASK_QUEUE)
public class OrderWorkflowImpl implements OrderWorkflow {

    private static final Logger log = Workflow.getLogger(OrderWorkflowImpl.class);

    private final OrderActivities activities = Workflow.newActivityStub(OrderActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .setMaximumAttempts(4)
                            .build())
                    .build());

    private final CompensationActivities compensations = Workflow.newActivityStub(CompensationActivities.class,
            ActivityOptions.newBuilder()
                    .setStartToCloseTimeout(Duration.ofSeconds(30))
                    .setRetryOptions(RetryOptions.newBuilder()
                            .setInitialInterval(Duration.ofSeconds(2))
                            .setBackoffCoefficient(2.0)
                            .setMaximumInterval(Duration.ofSeconds(30))
                            .setMaximumAttempts(10)
                            .build())
                    .build());

    private String currentStep = "STARTING";

    @Override
    public String processOrder(Long orderId, List<String> steps) {

        Saga saga = new Saga(new Saga.Options.Builder()
                .setParallelCompensation(false)
                .setContinueWithError(true)
                .build());

        try {
            for (String step : steps) {
                currentStep = step;
                switch (step) {
                    case WorkflowConstants.RESERVE_STOCK -> {
                        saga.addCompensation(compensations::releaseStockAndCancel, orderId);
                        activities.reserveStock(orderId);
                    }
                    case WorkflowConstants.PAYMENT -> {
                        String paymentId = activities.chargePayment(orderId);
                        saga.addCompensation(compensations::refundPayment, paymentId);
                    }
                    case WorkflowConstants.INVOICE -> activities.generateInvoice(orderId);
                    case WorkflowConstants.NOTIFY -> activities.sendNotification(orderId);
                    default -> throw ApplicationFailure.newNonRetryableFailure(
                            "Unknown workflow step: " + step, "BAD_CONFIG");
                }
            }
            currentStep = "COMPLETING";
            activities.completeOrder(orderId);
            currentStep = "DONE";
            return "COMPLETED";

        } catch (ActivityFailure e) {
            String reason = rootMessage(e);
            log.warn("Order {} got failed at step {}: {}", orderId, currentStep, reason);

            currentStep = "ROLLING_BACK";
            saga.compensate();
            compensations.markFailedIfPending(orderId);

            currentStep = "ROLLED_BACK";
            return "ROLLED_BACK: " + reason;
        }
    }

    @Override
    public String currentStep() {
        return currentStep;
    }

    private static String rootMessage(Throwable t) {
        Throwable c = t;
        while (c.getCause() != null) c = c.getCause();
        return c.getMessage();
    }
}
