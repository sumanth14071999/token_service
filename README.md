# token-authorization-service

When a driver holds their RFID charging card against a charger, the CPO asks us in real time: **may this
card charge here, right now?** This service answers.

```
 charger ──▶ CPO ──OCPI real-time authorization──▶ ocpi-adapter ──GET /api/v1/tokens/{uid}/authorization──▶ token-authorization-service ──▶ PostgreSQL
                                                                                                                  ▲
                                                             customer-account-service ── Kafka: tokens.events ────┘
```

## Tech

Java 21 · Spring Boot 3.3 · Spring Kafka · Spring Data JPA · PostgreSQL 16 · **Liquibase** · JUnit 5 / Mockito / Testcontainers

## Running locally

```bash
docker compose up -d                  # Postgres on 5433, Kafka on 9092
mvn spring-boot:run                   # app on 8080; Liquibase creates the schema and seed tokens
curl -i "localhost:8080/api/v1/tokens/04A2B3C4D5E6F7/authorization?locationId=LOC-BER-HBF"
./scripts/send-token-event.sh '{"type":"TOKEN_BLOCKED","uid":"04A2B3C4D5E6F7","reason":"STOLEN"}'
mvn test                              # the integration test needs Docker
```

## Production

* **3 replicas** behind a load balancer. ~4 million tokens. ~150 authorizations/s at peak.
* The CPO is waiting while we answer: the OCPI adapter gives us **1 s**, and our target is p99 < 300 ms.
  If we don't answer in time, many CPOs fall back to their own offline whitelist, which usually means they
  **let the driver charge**.
* The in-memory token cache (TTL 10 min) was added after a slow-database incident last winter.

---

## Authorization rules

`GET /api/v1/tokens/{uid}/authorization?locationId=...`

| Situation                          | Response                                                     |
|------------------------------------|--------------------------------------------------------------|
| Token unknown                      | `404`                                                        |
| Token blocked                      | `200`, `result: BLOCKED`                                     |
| Token expired                      | `200`, `result: EXPIRED`                                     |
| Otherwise                          | `200`, `result: ALLOWED`, with a new `authorizationReference`|

`BLOCKED` takes precedence over `EXPIRED`.

### Expiry

`validUntil` is the date printed on the card. A card is valid **through the end of that day, in German
time (Europe/Berlin)**. All our cards are issued in Germany. `null` means the card does not expire.

### Blocking (security requirement)

When customer service blocks a card (lost or stolen), **every authorization must refuse it within 30
seconds**, on every replica.

## Token events (`tokens.events`)

* Produced by customer-account-service. Record key = token `uid`. Delivery is at-least-once.
* JSON with a `type` discriminator: `TOKEN_ISSUED` (uid, contractId, validUntil), `TOKEN_BLOCKED` (uid,
  reason), `TOKEN_UNBLOCKED` (uid).
