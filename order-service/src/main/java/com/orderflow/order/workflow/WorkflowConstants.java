package com.orderflow.order.workflow;

public final class WorkflowConstants {

    private WorkflowConstants() {}

    public static final String TASK_QUEUE = "ORDER_TASK_QUEUE";

    public static final String RESERVE_STOCK = "RESERVE_STOCK";
    public static final String PAYMENT = "PAYMENT";
    public static final String INVOICE = "INVOICE";
    public static final String NOTIFY = "NOTIFY";
}
