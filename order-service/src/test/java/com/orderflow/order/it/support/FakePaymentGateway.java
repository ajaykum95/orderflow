package com.orderflow.order.it.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class FakePaymentGateway {

    public enum Mode { OK, DECLINE, DOWN }

    private final MockWebServer server = new MockWebServer();
    private final ObjectMapper mapper = new ObjectMapper();

    private volatile Mode mode = Mode.OK;
    private final List<String> chargeKeys = new CopyOnWriteArrayList<>();
    private final List<String> refundedIds = new CopyOnWriteArrayList<>();
    private final Map<String, Long> orderByPaymentId = new ConcurrentHashMap<>();

    public void start() {
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                try {
                    return handle(request);
                } catch (Exception e) {
                    return new MockResponse().setResponseCode(500).setBody(String.valueOf(e));
                }
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public String baseUrl() {
        return "http://localhost:" + server.getPort();
    }

    public void setMode(Mode mode) { this.mode = mode; }

    public void reset() {
        mode = Mode.OK;
        chargeKeys.clear();
        refundedIds.clear();
    }

    public List<String> chargeKeys() { return List.copyOf(chargeKeys); }

    public List<String> refundedPaymentIds() { return List.copyOf(refundedIds); }

    private MockResponse handle(RecordedRequest request) throws IOException {
        String path = request.getPath();

        if ("/api/payments".equals(path)) {
            chargeKeys.add(request.getHeader("Idempotency-Key"));
            if (mode == Mode.DOWN) {
                return new MockResponse().setResponseCode(503);
            }
            JsonNode body = mapper.readTree(request.getBody().readUtf8());
            long orderId = body.get("orderId").asLong();
            String paymentId = "pay_" + orderId;
            orderByPaymentId.put(paymentId, orderId);
            return json(payment(paymentId, orderId, new BigDecimal(body.get("amount").asText()),
                    body.get("currency").asText(), mode == Mode.DECLINE ? "DECLINED" : "SUCCESS"));
        }

        if (path != null && path.startsWith("/api/payments/") && path.endsWith("/refund")) {
            if (mode == Mode.DOWN) {
                return new MockResponse().setResponseCode(503);
            }
            String paymentId = path.substring("/api/payments/".length(), path.length() - "/refund".length());
            Long orderId = orderByPaymentId.get(paymentId);
            if (orderId == null) {
                return new MockResponse().setResponseCode(404);
            }
            refundedIds.add(paymentId);
            return json(payment(paymentId, orderId, BigDecimal.ZERO, "INR", "REFUNDED"));
        }

        return new MockResponse().setResponseCode(404);
    }

    private ObjectNode payment(String paymentId, long orderId, BigDecimal amount, String currency, String status) {
        ObjectNode n = mapper.createObjectNode();
        n.put("paymentId", paymentId);
        n.put("orderId", orderId);
        n.put("amount", amount);
        n.put("currency", currency);
        n.put("status", status);
        n.put("createdAt", Instant.now().toString());
        return n;
    }

    private MockResponse json(ObjectNode body) {
        return new MockResponse()
                .setResponseCode(200)
                .setHeader("Content-Type", "application/json")
                .setBody(body.toString());
    }
}
