package com.orderflow.order.it;

import com.fasterxml.jackson.databind.JsonNode;
import com.orderflow.order.it.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ApiDocsIT extends IntegrationTestBase {

    @Test
    void apiDocsArePublicAndDescribeTheMainEndpoints() throws Exception {
        MvcResult res = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
        JsonNode docs = json(res);

        JsonNode paths = docs.get("paths");
        assertThat(paths.has("/api/auth/login")).isTrue();
        assertThat(paths.has("/api/orders")).isTrue();
        assertThat(paths.has("/api/orders/{orderId}/invoice/pdf")).isTrue();
        assertThat(paths.has("/api/admin/audit/orders/{orderId}")).isTrue();

        assertThat(docs.at("/components/securitySchemes/bearerAuth/scheme").asText()).isEqualTo("bearer");
    }

    @Test
    void idempotencyKeyHeaderIsDocumentedAsRequired() throws Exception {
        JsonNode docs = json(mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn());

        boolean found = false;
        for (JsonNode p : docs.at("/paths/~1api~1orders/post/parameters")) {
            if ("Idempotency-Key".equals(p.get("name").asText())) {
                found = true;
                assertThat(p.get("required").asBoolean()).isTrue();
            }
        }
        assertThat(found).as("POST /api/orders supports the Idempotency-Key header").isTrue();
    }

    @Test
    void swaggerUiPageIsReachableWithoutLogin() throws Exception {
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }

    @Test
    void unknownUrlIs404InOurFormatNotAnInternalError() throws Exception {
        mvc.perform(get("/api/does-not-exist").header("Authorization", bearer(customerToken())))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(404));
    }

    @Test
    void wrongMethodIs405InOurFormat() throws Exception {
        mvc.perform(delete("/api/orders").header("Authorization", bearer(customerToken())))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }
}
