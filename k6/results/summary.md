| Metric | Value |
|---|---|
| Test duration | 64 s |
| Load | browse 20/s + new orders 2/s (20 customers) |
| Total HTTP requests | 1485 (23.3 req/s) |
| Failed HTTP requests | 0.00 % |
| Browse products: p50 / p95 / p99 | 3 ms / 6 ms / 7 ms |
| Create order (API response): p50 / p95 / p99 | 19 ms / 22 ms / 25 ms |
| Orders fully completed | 121 (1.90/s) |
| Orders not completed | 0 |
| Order end-to-end (create to COMPLETED): p50 / p95 / p99 | 524 ms / 527 ms / 534 ms |
| Checks passed | 100.00 % |

_End-to-end time is measured by polling every 0.5 s, so it is accurate to about half a second._
