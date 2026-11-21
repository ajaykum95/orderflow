package com.orderflow.order.workflow.activities;

import io.temporal.activity.ActivityInterface;

// Rollback वाले काम। इन्हें ज़्यादा बार retry करने दिया जाता है, क्योंकि rollback अधूरा नहीं रहना चाहिए।
@ActivityInterface
public interface CompensationActivities {

    void releaseStockAndCancel(Long orderId);

    void refundPayment(String paymentId);

    void markFailedIfPending(Long orderId);
}
