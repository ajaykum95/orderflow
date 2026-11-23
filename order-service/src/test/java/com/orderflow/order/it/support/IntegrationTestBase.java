package com.orderflow.order.it.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class IntegrationTestBase {

    protected static final FakePaymentGateway GATEWAY = new FakePaymentGateway();

    private static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16"));
    private static final GenericContainer<?> MONGO =
            new GenericContainer<>(DockerImageName.parse("mongo:7")).withExposedPorts(27017);
    private static final GenericContainer<?> TEMPORAL =
            new GenericContainer<>(DockerImageName.parse("temporalio/temporal:latest"))
                    .withCommand("server", "start-dev", "--ip", "0.0.0.0")
                    .withExposedPorts(7233)
                    .withStartupTimeout(Duration.ofMinutes(3));

    static {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        POSTGRES.start();
        MONGO.start();
        TEMPORAL.start();
        waitForTemporal();
        GATEWAY.start();
    }

    private static void waitForTemporal() {
        long deadline = System.nanoTime() + Duration.ofSeconds(90).toNanos();
        while (System.nanoTime() < deadline) {
            try {
                if (TEMPORAL.execInContainer("temporal", "operator", "cluster", "health").getExitCode() == 0) {
                    return;
                }
            } catch (Exception ignored) {
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("spring.data.mongodb.uri",
                () -> "mongodb://" + MONGO.getHost() + ":" + MONGO.getMappedPort(27017) + "/orderflow");
        r.add("spring.temporal.connection.target",
                () -> TEMPORAL.getHost() + ":" + TEMPORAL.getMappedPort(7233));
        r.add("app.payment.base-url", GATEWAY::baseUrl);
    }

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper mapper;
    @Autowired protected JdbcTemplate jdbc;
    @Autowired private CircuitBreakerRegistry circuitBreakers;

    @AfterEach
    void resetSharedState() {
        GATEWAY.reset();
        circuitBreakers.circuitBreaker("paymentService").reset();
    }

    protected static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    protected String register(String email, String password) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isCreated());
        return email;
    }

    protected String login(String email, String password) throws Exception {
        MvcResult res = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of("email", email, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return json(res).at("/data/token").asText();
    }

    protected String customerToken() throws Exception {
        String email = "cust-" + uid() + "@test.com";
        register(email, "password123");
        return login(email, "password123");
    }

    protected String adminToken() throws Exception {
        String email = "admin-" + uid() + "@test.com";
        register(email, "password123");
        jdbc.update("update users set role = 'ADMIN' where email = ?", email);
        return login(email, "password123");
    }

    protected long createProduct(String adminToken, String name, String price, int stock) throws Exception {
        MvcResult res = mvc.perform(post("/api/admin/products")
                        .header("Authorization", bearer(adminToken))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "name", name, "description", "test", "price", new java.math.BigDecimal(price),
                                "stock", stock))))
                .andExpect(status().isCreated())
                .andReturn();
        return json(res).at("/data/id").asLong();
    }

    protected int stockOf(long productId) throws Exception {
        MvcResult res = mvc.perform(get("/api/products/" + productId)).andExpect(status().isOk()).andReturn();
        return json(res).at("/data/stock").asInt();
    }

    protected MvcResult placeOrder(String token, String idempotencyKey, long productId, int quantity) throws Exception {
        return mvc.perform(post("/api/orders")
                        .header("Authorization", bearer(token))
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(mapper.writeValueAsString(Map.of(
                                "items", List.of(Map.of("productId", productId, "quantity", quantity))))))
                .andReturn();
    }

    protected long orderIdOf(MvcResult placed) throws Exception {
        return json(placed).at("/data/id").asLong();
    }

    protected String orderStatus(String token, long orderId) throws Exception {
        MvcResult res = mvc.perform(get("/api/orders/" + orderId).header("Authorization", bearer(token)))
                .andExpect(status().isOk()).andReturn();
        return json(res).at("/data/status").asText();
    }

    protected void awaitStatus(String token, long orderId, String expected, Duration timeout) {
        await().atMost(timeout).pollInterval(Duration.ofMillis(500)).untilAsserted(() ->
                assertThat(orderStatus(token, orderId)).as("order %d की हालत", orderId).isEqualTo(expected));
    }

    protected List<String> auditTypes(String adminToken, long orderId) throws Exception {
        List<String> types = new ArrayList<>();
        for (JsonNode e : auditTimeline(adminToken, orderId)) {
            types.add(e.get("type").asText());
        }
        return types;
    }

    protected JsonNode auditTimeline(String adminToken, long orderId) throws Exception {
        MvcResult res = mvc.perform(get("/api/admin/audit/orders/" + orderId)
                        .header("Authorization", bearer(adminToken)))
                .andExpect(status().isOk()).andReturn();
        return json(res).at("/data");
    }

    protected JsonNode auditEvent(String adminToken, long orderId, String type) throws Exception {
        for (JsonNode e : auditTimeline(adminToken, orderId)) {
            if (type.equals(e.get("type").asText())) return e;
        }
        return null;
    }

    protected void awaitAudit(String adminToken, long orderId, String type) {
        await().atMost(Duration.ofSeconds(15)).pollInterval(Duration.ofMillis(300)).untilAsserted(() ->
                assertThat(auditTypes(adminToken, orderId)).contains(type));
    }

    protected JsonNode json(MvcResult result) throws Exception {
        return mapper.readTree(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
    }

    protected static String bearer(String token) {
        return "Bearer " + token;
    }
}
