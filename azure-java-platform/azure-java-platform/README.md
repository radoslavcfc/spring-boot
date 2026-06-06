# azure-java-platform

End-to-end Java 17 sample for Azure with **Spring Boot Web API**, **Azure Functions (Java v4)**,
**Azure SQL** + **Cosmos DB**, **Event Hubs (Kafka)**, **Entra ID** auth, **Application Insights**,
**OpenAPI/Swagger**, **Terraform IaC**, and an **Azure DevOps** pipeline.

> **Note**: this scaffold was generated for you to build/run/deploy on your own machine and Azure
> subscription. Lovable did not run any of these commands.

## Layout
```
webapi/         Spring Boot 3 Web API → Azure SQL, long-running jobs, Swagger UI
functions/      Azure Functions Java v4 → HTTP, Kafka trigger, Cosmos, SQL, Cosmos seeder
shared/         Shared DTOs / events
infra/          Terraform: RG, SQL, Cosmos, Event Hubs, App Insights, Web App, Function App, Key Vault, identity grants
pipelines/      Azure DevOps YAML
local/          docker-compose for SQL Server + Redpanda (Kafka) + Cosmos emulator + Azurite
scripts/        Bootstrap helpers (tfstate storage, Entra app)
docs/           Architecture, run-local, deploy guides
```

## What goes where, and why

| Concern | Component | Reason |
|---|---|---|
| Sync CRUD over SQL | **Web API** | Stable HTTP surface, JPA, connection pools |
| Long-running operation (202 + polling) | **Web API** | Async executor + persisted job state |
| Bursty / event-driven HTTP intake → queue | **Functions HTTP trigger** | Pay-per-execution, scales to zero |
| Async processing of queued work | **Functions Kafka trigger** | Native binding to Event Hubs |
| Read/write Cosmos | **Functions** | Cosmos SDK + DefaultAzureCredential |
| First-run data seed (SQL) | Flyway migration `V2__seed.sql` | Versioned, idempotent |
| First-run data seed (Cosmos) | Timer-triggered Function with marker doc | Runs once, then no-ops |
| Auth | Entra ID JWT bearer on Web API + Functions | + Managed Identity for Azure resources |
| Observability | App Insights (Java agent on Web API, native on Functions) | Logs, traces, metrics |

## Quick start (Windows 11)

See [`docs/RUN_LOCAL.md`](docs/RUN_LOCAL.md). TL;DR:

```powershell
# Prereqs: JDK 17, Maven, Docker Desktop, Azure Functions Core Tools v4, Azure CLI
docker compose -f local/docker-compose.yml up -d
mvn -B clean install
# Terminal 1
mvn -pl webapi spring-boot:run -Dspring-boot.run.profiles=local
# Terminal 2
cd functions ; mvn azure-functions:run
```

Web API Swagger: http://localhost:8080/swagger-ui.html
Functions Swagger: http://localhost:7071/api/swagger

## Deploy to Azure

See [`docs/DEPLOY.md`](docs/DEPLOY.md). Summary:

1. `az login`
2. `bash scripts/bootstrap-tfstate.sh` (creates the remote state storage account + `infra/backend.hcl`)
3. `bash scripts/create-entra-app.sh` (creates app registration; note the `api://<app-id>`)
4. Edit `infra/envs/dev.tfvars` (set `entra_audience`, `sql_admin_password`)
5. `cd infra && terraform init -backend-config=backend.hcl && terraform apply -var-file=envs/dev.tfvars`
6. Push to Azure DevOps and run the pipeline (`pipelines/azure-pipelines.yml`)
7. Run `docs/POST_DEPLOY.md` SQL grants once

## License

MIT — sample code; review and harden before production use.
