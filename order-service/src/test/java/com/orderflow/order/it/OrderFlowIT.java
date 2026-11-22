package com.orderflow.order.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.orderflow.order.it.support.FakePaymentGateway.Mode;
import com.orderflow.order.it.support.IntegrationTestBase;
import com.orderflow.order.workflow.config.WorkflowProperties;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// पूरा सफ़र: API -> Temporal workflow -> stock / payment / invoice / email / audit, असली database के साथ।
// सिर्फ़ payment-service नकली है (FakePaymentGateway), ताकि हम उसे जब चाहें बिगाड़ सकें।
class OrderFlowIT extends IntegrationTestBase {

    private static final Duration FAST = Duration.ofSeconds(45);
    private static final Duration SLOW = Duration.ofSeconds(120);   // gateway बंद वाले test में retry का समय लगता है

    @Autowired WorkflowProperties workflowProperties;

    @Test
    void happyPathCompletesWithStockInvoiceAndAudit() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Phone-" + uid(), "500.00", 10);

        MvcResult placed = placeOrder(cust, "key-" + uid(), product, 2);
        status().isCreated().match(placed);
        assertThat(json(placed).at("/data/status").asText()).isEqualTo("PENDING");   // जवाब तुरंत, काम पीछे
        assertThat(json(placed).at("/data/totalAmount").decimalValue()).isEqualByComparingTo("1000.00");
        long orderId = orderIdOf(placed);

        awaitStatus(cust, orderId, "COMPLETED", FAST);

        assertThat(stockOf(product)).isEqualTo(8);
        assertThat(GATEWAY.chargeKeys()).containsExactly("order-" + orderId + "-charge");

        // invoice
        MvcResult invoice = mvc.perform(get("/api/orders/" + orderId + "/invoice")
                .header("Authorization", bearer(cust))).andExpect(status().isOk()).andReturn();
        JsonNode inv = json(invoice).at("/data");
        assertThat(inv.get("invoiceNumber").asText()).matches("INV-\\d{4}-\\d{2}-\\d{6}");
        assertThat(inv.get("totalAmount").decimalValue()).isEqualByComparingTo("1000.00");
        // टैक्स से पहले की रकम + टैक्स = कुल, एक पैसा भी इधर-उधर नहीं
        assertThat(inv.get("taxableAmount").decimalValue().add(inv.get("totalTax").decimalValue()))
                .isEqualByComparingTo(inv.get("totalAmount").decimalValue());

        // PDF सच में PDF है
        byte[] pdf = mvc.perform(get("/api/orders/" + orderId + "/invoice/pdf")
                        .header("Authorization", bearer(cust)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(new String(pdf, 0, 4, StandardCharsets.ISO_8859_1)).isEqualTo("%PDF");

        // audit की पूरी कहानी, सही क्रम में
        awaitAudit(admin, orderId, "ORDER_COMPLETED");
        assertThat(auditTypes(admin, orderId)).containsExactly(
                "ORDER_CREATED", "STOCK_RESERVED", "PAYMENT_SUCCEEDED",
                "INVOICE_ISSUED", "NOTIFICATION_SENT", "ORDER_COMPLETED");
    }

    @Test
    void sameIdempotencyKeyNeverCreatesASecondOrderOrASecondCharge() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Once-" + uid(), "500.00", 10);
        String key = "key-" + uid();

        MvcResult first = placeOrder(cust, key, product, 1);
        MvcResult second = placeOrder(cust, key, product, 1);

        status().isCreated().match(first);
        status().isOk().match(second);                                  // 201 के बाद 200
        assertThat(orderIdOf(second)).isEqualTo(orderIdOf(first));      // वही order
        long orderId = orderIdOf(first);

        awaitStatus(cust, orderId, "COMPLETED", FAST);
        assertThat(stockOf(product)).isEqualTo(9);                      // 8 नहीं: stock एक ही बार घटा
        assertThat(GATEWAY.chargeKeys()).hasSize(1);                    // पैसा एक ही बार माँगा
    }

    @Test
    void invoiceNumbersAreConsecutiveWithNoGaps() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Seq-" + uid(), "100.00", 20);

        List<Integer> serials = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            long orderId = orderIdOf(placeOrder(cust, "key-" + uid(), product, 1));
            awaitStatus(cust, orderId, "COMPLETED", FAST);
            MvcResult invoice = mvc.perform(get("/api/orders/" + orderId + "/invoice")
                    .header("Authorization", bearer(cust))).andReturn();
            String number = json(invoice).at("/data/invoiceNumber").asText();
            serials.add(Integer.parseInt(number.substring(number.lastIndexOf('-') + 1)));
        }

        assertThat(serials.get(1)).isEqualTo(serials.get(0) + 1);
        assertThat(serials.get(2)).isEqualTo(serials.get(1) + 1);
    }

    @Test
    void declinedPaymentRollsBackStockAndCancelsTheOrder() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Declined-" + uid(), "500.00", 10);
        GATEWAY.setMode(Mode.DECLINE);

        long orderId = orderIdOf(placeOrder(cust, "key-" + uid(), product, 2));

        awaitStatus(cust, orderId, "CANCELLED", FAST);
        assertThat(stockOf(product)).isEqualTo(10);                     // stock पूरा वापस

        awaitAudit(admin, orderId, "ORDER_ROLLED_BACK");
        List<String> types = auditTypes(admin, orderId);
        assertThat(types).contains("STOCK_RESERVED", "STOCK_RELEASED", "ORDER_ROLLED_BACK");
        assertThat(types).doesNotContain("PAYMENT_SUCCEEDED", "PAYMENT_REFUNDED");   // पैसा कटा ही नहीं था

        JsonNode rollback = auditEvent(admin, orderId, "ORDER_ROLLED_BACK").get("details");
        assertThat(rollback.get("failedStep").asText()).isEqualTo("PAYMENT");
        assertThat(rollback.get("reason").asText()).isEqualTo("Payment declined");
        assertThat(rollback.get("finalStatus").asText()).isEqualTo("CANCELLED");

        // decline पर retry नहीं होता: gateway को सिर्फ़ एक बार बुलाया गया
        assertThat(GATEWAY.chargeKeys()).hasSize(1);
    }

    @Test
    void notEnoughStockFailsTheOrderWithoutTouchingStockOrPayment() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Scarce-" + uid(), "500.00", 3);

        long orderId = orderIdOf(placeOrder(cust, "key-" + uid(), product, 5));

        awaitStatus(cust, orderId, "FAILED", FAST);                     // CANCELLED नहीं, FAILED
        assertThat(stockOf(product)).isEqualTo(3);
        assertThat(GATEWAY.chargeKeys()).isEmpty();                     // payment तक पहुँचे ही नहीं

        awaitAudit(admin, orderId, "ORDER_ROLLED_BACK");
        assertThat(auditTypes(admin, orderId)).containsExactly("ORDER_CREATED", "ORDER_ROLLED_BACK");
        JsonNode rollback = auditEvent(admin, orderId, "ORDER_ROLLED_BACK").get("details");
        assertThat(rollback.get("failedStep").asText()).isEqualTo("RESERVE_STOCK");
        assertThat(rollback.get("reason").asText()).startsWith("Insufficient stock");
        assertThat(rollback.get("finalStatus").asText()).isEqualTo("FAILED");
    }

    @Test
    void failureAfterPaymentRefundsFirstAndThenReleasesStock() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Refund-" + uid(), "500.00", 10);
        workflowProperties.setSimulateInvoiceFailure(true);   // INVOICE जानबूझकर फेल
        long orderId;
        try {
            orderId = orderIdOf(placeOrder(cust, "key-" + uid(), product, 1));
            awaitStatus(cust, orderId, "CANCELLED", FAST);
            awaitAudit(admin, orderId, "ORDER_ROLLED_BACK");
        } finally {
            workflowProperties.setSimulateInvoiceFailure(false);
        }

        assertThat(stockOf(product)).isEqualTo(10);
        assertThat(GATEWAY.refundedPaymentIds()).containsExactly("pay_" + orderId);

        // उल्टे क्रम का सबूत: refund पहले, stock वापस बाद में
        List<String> types = auditTypes(admin, orderId);
        assertThat(types).contains("PAYMENT_SUCCEEDED", "PAYMENT_REFUNDED", "STOCK_RELEASED");
        assertThat(types.indexOf("PAYMENT_REFUNDED")).isLessThan(types.indexOf("STOCK_RELEASED"));
        assertThat(auditEvent(admin, orderId, "ORDER_ROLLED_BACK").get("details").get("failedStep").asText())
                .isEqualTo("INVOICE");

        // रद्द order का invoice नहीं बना
        mvc.perform(get("/api/orders/" + orderId + "/invoice").header("Authorization", bearer(cust)))
                .andExpect(status().isNotFound());
    }

    // धीमा test (लगभग 20-30 सेकंड): Temporal कई बार कोशिश करता है, फिर rollback
    @Test
    void paymentServiceOutageIsRetriedWithTheSameKeyThenRolledBack() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Outage-" + uid(), "500.00", 10);
        GATEWAY.setMode(Mode.DOWN);

        long orderId = orderIdOf(placeOrder(cust, "key-" + uid(), product, 1));

        awaitStatus(cust, orderId, "CANCELLED", SLOW);
        assertThat(stockOf(product)).isEqualTo(10);

        // कई बार कोशिश हुई, और हर बार वही key: इसलिए असली gateway पर पैसा दो बार कभी न कटता
        assertThat(GATEWAY.chargeKeys()).hasSizeGreaterThanOrEqualTo(3);
        assertThat(GATEWAY.chargeKeys()).containsOnly("order-" + orderId + "-charge");

        awaitAudit(admin, orderId, "ORDER_ROLLED_BACK");
        assertThat(auditEvent(admin, orderId, "ORDER_ROLLED_BACK").get("details").get("failedStep").asText())
                .isEqualTo("PAYMENT");
    }

    @Test
    void ordersAndInvoicesAreVisibleOnlyToTheirOwnerAndAdmins() throws Exception {
        String admin = adminToken(), owner = customerToken(), stranger = customerToken();
        long product = createProduct(admin, "Private-" + uid(), "500.00", 10);
        long orderId = orderIdOf(placeOrder(owner, "key-" + uid(), product, 1));
        awaitStatus(owner, orderId, "COMPLETED", FAST);

        // दूसरे को 404 (403 नहीं), ताकि पता भी न चले कि order मौजूद है
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + orderId + "/invoice").header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/orders/" + orderId + "/invoice/pdf").header("Authorization", bearer(stranger)))
                .andExpect(status().isNotFound());

        // मालिक और admin देख सकते हैं
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(owner)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/orders/" + orderId + "/invoice").header("Authorization", bearer(admin)))
                .andExpect(status().isOk());
    }

    @Test
    void priceComesFromTheCatalogNotFromTheClient() throws Exception {
        String admin = adminToken(), cust = customerToken();
        long product = createProduct(admin, "Priced-" + uid(), "500.00", 10);

        // client चालाकी से अपनी कीमत भेजे, तो उसे अनदेखा किया जाना चाहिए
        MvcResult placed = mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/orders")
                        .header("Authorization", bearer(cust))
                        .header("Idempotency-Key", "key-" + uid())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"items\":[{\"productId\":" + product + ",\"quantity\":1,\"price\":1}]}"))
                .andExpect(status().isCreated()).andReturn();

        assertThat(json(placed).at("/data/totalAmount").decimalValue()).isEqualByComparingTo(new BigDecimal("500.00"));
        awaitStatus(cust, orderIdOf(placed), "COMPLETED", FAST);
    }
}
