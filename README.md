# OrderFlow
Order -> Stock -> Payment -> Invoice -> Email (rollback on failure)

## How to Run
1. docker compose up -d
2. cd order-service && mvn spring-boot:run

## Module Status
- [ ] 1 Auth  - [ ] 2 Catalog  - [ ] 3 Order  - [ ] 4 Payment
- [ ] 5 Saga  - [ ] 6 Invoice  - [ ] 7 Notification  - [ ] 8 Audit
- [ ] 9 Tests - [ ] 10 Docs    - [ ] 11 Deploy

<!-- Add this at the very top of README.md, right below the title.
     Replace <USER> and <REPO> with your GitHub username and repository name -->

[![CI](https://github.com/<USER>/<REPO>/actions/workflows/ci.yml/badge.svg)](https://github.com/<USER>/<REPO>/actions/workflows/ci.yml)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

---

<!-- Add this section to the rest of the README -->

## Running Tests

| What | Command | Time | Docker Required? |
|---|---|---|---|
| Fast unit tests | `cd order-service && mvn test` | A few seconds | No |
| All tests + coverage | `cd order-service && mvn verify` | 3-5 minutes | Yes |
| payment-service tests | `cd payment-service && mvn verify` | A few seconds | No |

Coverage report: `order-service/target/site/jacoco/index.html`

### How Tests Are Structured

- **Unit tests** (`*Test.java`): Without a database, fast. Covers state rules, tax calculations, invoice numbers, JWT, and email logic.
- **Integration tests** (`*IT.java`): Uses real Postgres, MongoDB, and Temporal in Docker (Testcontainers). Tests the complete flow from API to workflow, replacing `payment-service` with a mock gateway that tests can intentionally break/fail on demand.
- After every test, the gateway and circuit breaker are reset to their normal state so tests do not interfere with one another.
