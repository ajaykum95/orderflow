package com.orderflow.order.workflow.config;

import com.orderflow.order.workflow.WorkflowConstants;
import jakarta.annotation.PostConstruct;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;

@Component
@ConfigurationProperties(prefix = "app.workflow")
public class WorkflowProperties {

    private static final Set<String> KNOWN = Set.of(
            WorkflowConstants.RESERVE_STOCK, WorkflowConstants.PAYMENT,
            WorkflowConstants.INVOICE, WorkflowConstants.NOTIFY);

    private List<String> steps = List.of(
            WorkflowConstants.RESERVE_STOCK, WorkflowConstants.PAYMENT,
            WorkflowConstants.INVOICE, WorkflowConstants.NOTIFY);

    private boolean simulateInvoiceFailure = false;

    @PostConstruct
    void validate() {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalStateException("app.workflow.steps should not be empty");
        }
        for (String s : steps) {
            if (!KNOWN.contains(s)) {
                throw new IllegalStateException("Unknown workflow step: " + s + " (Known: " + KNOWN + ")");
            }
        }
        int reserve = steps.indexOf(WorkflowConstants.RESERVE_STOCK);
        int pay = steps.indexOf(WorkflowConstants.PAYMENT);
        if (reserve < 0 || pay < 0 || reserve > pay) {
            throw new IllegalStateException("RESERVE_STOCK AND PAYMENT BOTH ARE IMPORTANT, AND RESERVE_STOCK SHOULD BE COME FIRST");
        }
    }

    public List<String> getSteps() { return steps; }
    public void setSteps(List<String> steps) { this.steps = steps; }
    public boolean isSimulateInvoiceFailure() { return simulateInvoiceFailure; }
    public void setSimulateInvoiceFailure(boolean v) { this.simulateInvoiceFailure = v; }
}
