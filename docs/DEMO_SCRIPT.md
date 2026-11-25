# Demo video script (about 3 minutes)

Goal: show a non-technical client that the system **does the right thing when things go wrong**. Keep the focus on the failure scenarios; that is what sets this project apart.

## Before you record

- Start everything: `docker compose up -d`, `payment-service`, `order-service`.
- Browser tabs ready, in this order: **Swagger UI** (8080), **Temporal UI** (8233), **MailHog** (8025), and the **README** on GitHub.
- Create the admin user and log in; have one product ready (Phone, price 500, stock 10) so you are not typing during the recording.
- Reset the mock gateway: `PUT http://localhost:8081/api/chaos` with `{"failureRate":0,"slowRate":0}`.
- Close anything showing passwords, tokens or personal data. Use 1080p, and zoom the browser to 125 percent so text is readable.
- Tools: OBS Studio (free) or the Windows Game Bar (`Win + G`). Record the screen and your voice; a face camera is optional.

## Storyboard

| Time | Show | Say (suggested wording) |
|---|---|---|
| 0:00 | README title and the architecture diagram | "This is OrderFlow, an order-to-cash backend. A customer orders, we reserve stock, take payment, issue a GST invoice and email it. The interesting part is what happens when something fails." |
| 0:20 | Swagger UI: place an order (Phone, quantity 2) | "I place an order. The API answers instantly with PENDING; the real work runs in the background as a workflow." |
| 0:35 | `GET` the order until `COMPLETED`; then the invoice PDF; then MailHog | "A second later it is complete. Stock is reduced, there is a numbered tax invoice, and the customer got an email with the PDF attached." |
| 0:55 | Temporal UI: open the workflow, show the step history | "Every step is recorded here. If the server crashes mid-order, it resumes exactly where it stopped." |
| 1:10 | Send the **same Idempotency-Key** again | "If the customer double-clicks, or the network retries, the same key returns the same order. Never two orders, never two charges." |
| 1:25 | Order above INR 100,000 (2 laptops): declined, then `CANCELLED`; show stock restored | "Here the payment is declined. The system automatically puts the stock back and tells the customer. Nothing is left half-done." |
| 1:50 | `GET /api/admin/audit/orders/{id}` for that order | "Every event is recorded in an audit trail: what happened, when, and why it was rolled back." |
| 2:05 | Postman or Swagger: `PUT :8081/api/chaos {"failureRate":1.0}`, then place an order | "Now I break the payment provider on purpose. The system retries, then a circuit breaker stops hammering a dead service, and the order is cancelled cleanly with stock restored." |
| 2:30 | `GET /api/admin/payments/circuit`: state `OPEN`; reset the gateway | "The breaker is open. When the provider recovers, it closes again by itself." |
| 2:40 | GitHub: Actions tab (green CI), the coverage badge, `mvn verify` output if you have it | "All of this is covered by automated tests that run against real databases on every commit." |
| 2:50 | README performance table | "And here are the measured numbers from a load test." |
| 3:00 | README top | "The code, the Postman collection and the load test are all in the repository. Happy to adapt this to your workflow." |

## Tips

- Rehearse once with the timer, then record in one take; small stumbles sound human. Trim only the beginning and end.
- If an order takes longer than expected to complete, just narrate the Temporal UI while you wait.
- Upload as **Unlisted** on YouTube and put the link at the top of the README and in your Upwork portfolio item.
- Do not read the screen aloud. Say what each thing proves for the client.
