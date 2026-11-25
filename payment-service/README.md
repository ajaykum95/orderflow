# payment-service (mock gateway)

To run: `mvn spring-boot:run` (port 8081)

| Method | URL | Action |
|---|---|---|
| POST | /api/payments | Charge (header: Idempotency-Key) |
| GET  | /api/payments/{id} | Get payment details |
| POST | /api/payments/{id}/refund | Refund |
| GET/PUT | /api/chaos | Change failure-rate / slow-rate (for demo) |

Rule: If amount > 100000, return DECLINED (business decline, will not be retried).
