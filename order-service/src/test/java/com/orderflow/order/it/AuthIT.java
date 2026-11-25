package com.orderflow.order.it;

import com.orderflow.order.it.support.IntegrationTestBase;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    @Test
    void tamperedTokenIsRejected() throws Exception {
        String token = customerToken();

        mvc.perform(get("/api/orders").header("Authorization", bearer(token)))
                .andExpect(status().isOk());

        mvc.perform(get("/api/orders").header("Authorization", bearer(flipInsideSignature(token))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void customerCannotForgeAnAdminRoleIntoTheirToken() throws Exception {
        String token = customerToken();
        String[] parts = token.split("\\.");
        String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        String forgedPayload = payload.replace("\"CUSTOMER\"", "\"ADMIN\"");
        assertThat(forgedPayload).as("role updated to").isNotEqualTo(payload);

        String forged = parts[0] + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(forgedPayload.getBytes(StandardCharsets.UTF_8))
                + "." + parts[2];

        mvc.perform(get("/api/admin/audit/orders/1").header("Authorization", bearer(forged)))
                .andExpect(status().isUnauthorized());
    }

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

    private static String flipInsideSignature(String token) {
        int i = token.lastIndexOf('.') + 1 + 5;
        char replacement = token.charAt(i) == 'A' ? 'B' : 'A';
        return token.substring(0, i) + replacement + token.substring(i + 1);
    }
}