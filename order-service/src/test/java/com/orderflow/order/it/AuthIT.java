package com.orderflow.order.it;

import com.orderflow.order.it.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AuthIT extends IntegrationTestBase {

    @Test
    void registerThenLoginReturnsAToken() throws Exception {
        String email = "u-" + uid() + "@test.com";
        register(email, "password123");

        String token = login(email, "password123");

        assertThat(token).isNotBlank().contains(".");
    }

    @Test
    void registeringTheSameEmailTwiceIsAConflict() throws Exception {
        String email = "u-" + uid() + "@test.com";
        register(email, "password123");

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", "password123"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(409));
    }

    @Test
    void invalidRegistrationListsTheProblems() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "not-an-email", "password", "short"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors.length()").value(2));
    }

    @Test
    void wrongPasswordIsUnauthorizedAndDoesNotRevealWhichPartWasWrong() throws Exception {
        String email = "u-" + uid() + "@test.com";
        register(email, "password123");

        MvcResult wrongPassword = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", "wrong-password"))))
                .andExpect(status().isUnauthorized()).andReturn();
        MvcResult unknownEmail = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", "nobody-" + uid() + "@test.com",
                                "password", "password123"))))
                .andExpect(status().isUnauthorized()).andReturn();

        assertThat(json(wrongPassword).get("message").asText())
                .isEqualTo(json(unknownEmail).get("message").asText());
    }

    @Test
    void protectedEndpointWithoutTokenIsUnauthorizedInOurFormat() throws Exception {
        mvc.perform(get("/api/orders"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void customerCannotReachAdminEndpoints() throws Exception {
        String token = customerToken();

        mvc.perform(get("/api/admin/audit/orders/1").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

   /* @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = customerToken();
        String tampered = token.substring(0, token.length() - 1) + (token.endsWith("A") ? "B" : "A");

        mvc.perform(get("/api/orders").header("Authorization", bearer(tampered)))
                .andExpect(status().isUnauthorized());
    }*/

    @Test
    void registrationNeverCreatesAnAdmin() throws Exception {
        String email = "sneaky-" + uid() + "@test.com";
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "email", email, "password", "password123", "role", "ADMIN"))))
                .andExpect(status().isCreated());
        String token = login(email, "password123");

        mvc.perform(get("/api/admin/audit/orders/1").header("Authorization", bearer(token)))
                .andExpect(status().isForbidden());
    }
}
