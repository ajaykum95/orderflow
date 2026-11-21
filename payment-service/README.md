# payment-service (mock gateway)

चलाने के लिए:  mvn spring-boot:run   (port 8081)

| Method | URL | काम |
|---|---|---|
| POST | /api/payments | charge (header: Idempotency-Key) |
| GET  | /api/payments/{id} | payment देखना |
| POST | /api/payments/{id}/refund | refund |
| GET/PUT | /api/chaos | failure-rate / slow-rate बदलना (demo के लिए) |

नियम: रकम > 100000 हो तो DECLINED (business decline, retry नहीं होगा)।
