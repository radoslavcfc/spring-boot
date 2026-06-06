# Run locally on Windows 11

## Prerequisites

Install (winget commands shown):

```powershell
winget install EclipseAdoptium.Temurin.17.JDK
winget install Apache.Maven
winget install Docker.DockerDesktop
winget install Microsoft.AzureCLI
winget install Microsoft.Azure.FunctionsCoreTools
```

Verify:
```powershell
java -version          # 17.x
mvn -v
func --version         # 4.x
az --version
docker --version
```

## 1. Start local infra

```powershell
cd local
docker compose up -d
```

This starts:
- **SQL Server 2022** on `localhost:1433` (sa / `Your_password123`), database `appdb`
- **Redpanda (Kafka API)** on `localhost:9092`
- **Cosmos DB Linux emulator** on `https://localhost:8081` (default emulator key)
- **Azurite** for Functions storage

The Cosmos emulator's HTTPS cert is self-signed. On first use you may need to trust it
(`docs/COSMOS_CERT.md` in the Microsoft docs). For quick local dev the SDK is configured
to accept the emulator's cert via the well-known emulator key.

Create the Kafka topic once:
```powershell
docker exec -it local-redpanda-1 rpk topic create orders --partitions 4
```

## 2. Build everything

```powershell
mvn -B clean install
```

## 3. Run the Web API

```powershell
mvn -pl webapi spring-boot:run "-Dspring-boot.run.profiles=local"
```

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Health: http://localhost:8080/actuator/health

Flyway runs `V1__init.sql` then `V2__seed.sql` on first start; subsequent restarts skip seeding.

For local development you may temporarily allow unauthenticated access by removing the
`oauth2ResourceServer` line in `SecurityConfig`, **or** set `ENTRA_ISSUER` / `ENTRA_AUDIENCE`
to your dev tenant and use a real bearer token from the Azure Portal "Try" experience.

## 4. Run the Functions app

```powershell
cd functions
mvn azure-functions:run
```

- HTTP base: http://localhost:7071/api
- Swagger: http://localhost:7071/api/swagger
- The Cosmos seeder timer runs once on startup and inserts 3 seed docs + a marker.

## 5. End-to-end smoke test

```powershell
# Submit an order (local: JWT validation will reject without a token; for local-only,
# comment out the JwtValidator.validate(...) check in SubmitOrderHttp.java)
curl -X POST http://localhost:7071/api/orders `
  -H "Content-Type: application/json" `
  -d '{"customerId":"c1","sku":"SKU-001","quantity":2}'

# Poll Cosmos for the processed doc
curl http://localhost:7071/api/orders/<correlationId>
```
