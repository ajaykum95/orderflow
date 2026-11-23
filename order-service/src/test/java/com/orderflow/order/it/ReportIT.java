package com.orderflow.order.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.orderflow.order.it.support.FakePaymentGateway.Mode;
import com.orderflow.order.it.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ReportIT extends IntegrationTestBase {

    private static final Duration FAST = Duration.ofSeconds(45);

    @Test
    void dailyReportCountsOrdersAndRanksProducts() throws Exception {
        String admin = adminToken(), cust = customerToken();
        String name = "Report-" + uid();
        long product = createProduct(admin, name, "5000000.00", 10);

        long o1 = orderIdOf(placeOrder(cust, "key-" + uid(), product, 1));
        long o2 = orderIdOf(placeOrder(cust, "key-" + uid(), product, 2));
        awaitStatus(cust, o1, "COMPLETED", FAST);
        awaitStatus(cust, o2, "COMPLETED", FAST);

        GATEWAY.setMode(Mode.DECLINE);
        long o3 = orderIdOf(placeOrder(cust, "key-" + uid(), product, 1));
        awaitStatus(cust, o3, "CANCELLED", FAST);

        LocalDate today = LocalDate.now(ZoneId.of("Asia/Kolkata"));
        MvcResult generated = mvc.perform(post("/api/admin/reports/daily").param("date", today.toString())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn();
        JsonNode report = json(generated).at("/data");

        assertThat(report.get("date").asText()).isEqualTo(today.toString());
        assertThat(report.get("completedOrders").asLong()).isGreaterThanOrEqualTo(2);
        assertThat(report.get("cancelledOrders").asLong()).isGreaterThanOrEqualTo(1);
        assertThat(report.get("totalOrders").asLong()).isGreaterThanOrEqualTo(3);
        assertThat(report.get("taxCollected").decimalValue().signum()).isPositive();

        JsonNode top = report.get("topProducts").get(0);
        assertThat(top.get("name").asText()).isEqualTo(name);
        assertThat(top.get("quantity").asLong()).isEqualTo(3);
        assertThat(top.get("revenue").decimalValue()).isEqualByComparingTo("15000000.00");

        MvcResult fetched = mvc.perform(get("/api/admin/reports/daily").param("date", today.toString())
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isOk()).andReturn();
        assertThat(json(fetched).at("/data/completedOrders").asLong()).isEqualTo(report.get("completedOrders").asLong());
    }

    @Test
    void reportForADayWithNoOrdersIsAllZeros() throws Exception {
        MvcResult res = mvc.perform(post("/api/admin/reports/daily").param("date", "2000-01-01")
                        .header("Authorization", bearer(adminToken())))
                .andExpect(status().isOk()).andReturn();
        JsonNode report = json(res).at("/data");

        assertThat(report.get("totalOrders").asLong()).isZero();
        assertThat(report.get("grossSales").decimalValue()).isEqualByComparingTo("0.00");
        assertThat(report.get("topProducts").size()).isZero();
    }

    @Test
    void unknownDateIs404AndBadDateIs400() throws Exception {
        String admin = adminToken();

        mvc.perform(get("/api/admin/reports/daily").param("date", "1999-12-31")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/admin/reports/daily").param("date", "abc")
                        .header("Authorization", bearer(admin)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void customersCannotSeeReports() throws Exception {
        mvc.perform(get("/api/admin/reports/daily").param("date", "2000-01-01")
                        .header("Authorization", bearer(customerToken())))
                .andExpect(status().isForbidden());
    }
}
