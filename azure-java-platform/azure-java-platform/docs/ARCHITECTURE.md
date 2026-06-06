# Architecture

```text
                +----------------+
                |   Entra ID     |  (JWT bearer + Managed Identities)
                +-------+--------+
                        |
        +---------------+----------------+
        |                                |
+-------v---------+              +-------v---------+
|   Web API       |              |  Function App   |
|  Spring Boot 3  |              |  Java v4        |
|  (App Service)  |              |  (Linux Y1)     |
|                 |              |                 |
|  /api/products  |              |  HTTP triggers  |
|  /api/jobs      |              |   POST /orders  |--push--> Event Hubs (Kafka)
|  Swagger UI     |              |   GET  /orders  |              |
+-------+---------+              |   /swagger      |              |
        | JPA                    |  Kafka trigger  |<-------------+
        |                        |   processes &   |
        v                        |   writes to     |--read/write-> Cosmos DB
+-------+---------+              |   Cosmos        |
|   Azure SQL     |<-------------+  Timer seeder   |
|   appdb         |   (MI)       |   (one-time)    |
+-----------------+              +--------+--------+
        ^                                 |
        | Flyway migrations               |
        | (V1 init, V2 seed-once)         |
        |                                 v
        +----- Application Insights / Log Analytics <----+
```

## Key choices
- **Web API for sync + long-running**: long-running uses 202 + `Location` polling; state in SQL.
- **Functions for HTTP intake → Kafka → async processing**: scales independently, decouples client latency from work duration.
- **Managed Identity everywhere**: no connection-string secrets in app settings (Event Hubs uses MI in production via Kafka OAUTHBEARER; the scaffold ships with SASL/PLAIN connection string for simplicity — swap to OAUTHBEARER for prod).
- **Seeding**:
  - SQL: Flyway versioned migration runs once.
  - Cosmos: Timer Function checks for `_seeded` marker doc; inserts seeds + marker on first run only.
