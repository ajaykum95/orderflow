### Performance

**Test environment:** Local machine (11th Gen Intel(R) Core(TM) i7-1165G7 @ 2.80GHz, 16GB RAM)

| Metric | Target / Scenario | Avg | Med | p(90) | p(95) | p(99) | Max |
| :--- | :--- | :--- | :--- | :--- | :--- | :--- | :--- |
| **Browse Products (`GET`)** | 50 req/s (1m) | 2.98 ms | 2.66 ms | 3.97 ms | 4.70 ms | 7.22 ms | 41.76 ms |
| **Create Order (`POST`)** | 5 req/s (1m) | 14.84 ms | 13.62 ms | 19.01 ms | 20.61 ms | 23.35 ms | 66.54 ms |
| **Order Completion Time** | Async end-to-end | 520.07 ms | 519.00 ms | 525.00 ms | 526.00 ms | 529.00 ms | 570.00 ms |
| **Overall HTTP Duration** | 3,645 total reqs (57.14/s) | 4.96 ms | 2.82 ms | 9.38 ms | 14.16 ms | 74.13 ms | 87.33 ms |

* **Throughput:** 57.14 requests/s (3,302 total iterations)
* **Orders Completed:** 301 orders (~4.72 orders/s)
* **Error Rate:** 0.00% (`http_req_failed`)
* **Checks Passed:** 100.00% (3,603 / 3,603)