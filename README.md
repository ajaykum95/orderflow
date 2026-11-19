# OrderFlow
Order -> Stock -> Payment -> Invoice -> Email (failure पर rollback)

## चलाने के लिए
1. docker compose up -d
2. cd order-service && mvn spring-boot:run

## Modules की स्थिति
- [ ] 1 Auth  - [ ] 2 Catalog  - [ ] 3 Order  - [ ] 4 Payment
- [ ] 5 Saga  - [ ] 6 Invoice  - [ ] 7 Notification  - [ ] 8 Audit
- [ ] 9 Tests - [ ] 10 Docs    - [ ] 11 Deploy
