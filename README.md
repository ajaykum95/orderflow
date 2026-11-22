# OrderFlow
Order -> Stock -> Payment -> Invoice -> Email (failure पर rollback)

## चलाने के लिए
1. docker compose up -d
2. cd order-service && mvn spring-boot:run

## Modules की स्थिति
- [ ] 1 Auth  - [ ] 2 Catalog  - [ ] 3 Order  - [ ] 4 Payment
- [ ] 5 Saga  - [ ] 6 Invoice  - [ ] 7 Notification  - [ ] 8 Audit
- [ ] 9 Tests - [ ] 10 Docs    - [ ] 11 Deploy

<!-- README.md में सबसे ऊपर, title के नीचे जोड़ें।
     <USER> और <REPO> की जगह अपना GitHub username और repo का नाम डालें -->

[![CI](https://github.com/<USER>/<REPO>/actions/workflows/ci.yml/badge.svg)](https://github.com/<USER>/<REPO>/actions/workflows/ci.yml)
![Coverage](.github/badges/jacoco.svg)
![Branches](.github/badges/branches.svg)

---

<!-- README के बाकी हिस्से में यह section जोड़ें -->

## Tests चलाना

| क्या | Command | समय | Docker चाहिए? |
|---|---|---|---|
| तेज़ unit tests | `cd order-service && mvn test` | कुछ सेकंड | नहीं |
| सारे tests + coverage | `cd order-service && mvn verify` | 3-5 मिनट | हाँ |
| payment-service के tests | `cd payment-service && mvn verify` | कुछ सेकंड | नहीं |

Coverage report: `order-service/target/site/jacoco/index.html`

### Tests कैसे बने हैं

- **Unit tests** (`*Test.java`): बिना database के, तेज़। हालतों के नियम, टैक्स का हिसाब, invoice नंबर, JWT, email का तर्क
- **Integration tests** (`*IT.java`): असली Postgres, MongoDB और Temporal Docker में (Testcontainers)। पूरा सफ़र API से workflow तक, payment-service की जगह एक नकली gateway जिसे test जब चाहे बिगाड़ सकता है
- हर test के बाद gateway और circuit breaker अपनी सामान्य हालत में लौटा दिए जाते हैं, ताकि tests एक-दूसरे को न बिगाड़ें
