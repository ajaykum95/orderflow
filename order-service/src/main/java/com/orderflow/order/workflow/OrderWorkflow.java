package com.orderflow.order.workflow;

import io.temporal.workflow.QueryMethod;
import io.temporal.workflow.WorkflowInterface;
import io.temporal.workflow.WorkflowMethod;

import java.util.List;

@WorkflowInterface
public interface OrderWorkflow {

    @WorkflowMethod
    String processOrder(Long orderId, List<String> steps);

    @QueryMethod
    String currentStep();
}
