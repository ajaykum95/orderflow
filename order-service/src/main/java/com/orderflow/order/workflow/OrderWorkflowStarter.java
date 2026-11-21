package com.orderflow.order.workflow;

import com.orderflow.order.workflow.config.WorkflowProperties;
import io.temporal.api.enums.v1.WorkflowIdReusePolicy;
import io.temporal.client.WorkflowClient;
import io.temporal.client.WorkflowExecutionAlreadyStarted;
import io.temporal.client.WorkflowOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;

@Component
public class OrderWorkflowStarter {

    private static final Logger log = LoggerFactory.getLogger(OrderWorkflowStarter.class);

    private final WorkflowClient client;
    private final WorkflowProperties props;

    public OrderWorkflowStarter(WorkflowClient client, WorkflowProperties props) {
        this.client = client;
        this.props = props;
    }

    public void start(Long orderId) {
        try {
            OrderWorkflow workflow = client.newWorkflowStub(OrderWorkflow.class,
                    WorkflowOptions.newBuilder()
                            .setTaskQueue(WorkflowConstants.TASK_QUEUE)
                            .setWorkflowId("order-" + orderId)
                            .setWorkflowIdReusePolicy(
                                    WorkflowIdReusePolicy.WORKFLOW_ID_REUSE_POLICY_REJECT_DUPLICATE)
                            .build());

            WorkflowClient.start(workflow::processOrder, orderId, new ArrayList<>(props.getSteps()));
            log.info("Order {} workflow started", orderId);

        } catch (WorkflowExecutionAlreadyStarted e) {
            log.info("Order {} workflow is already running", orderId);
        } catch (Exception e) {
            log.error("Order {} workflow couldn't start; order will be PENDING", orderId, e);
        }
    }
}
