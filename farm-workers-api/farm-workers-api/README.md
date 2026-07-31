# 🌾 Farm Seasonal Workers API
### A Spring Boot + Azure learning project for .NET developers

---

## 🗺️ .NET → Java Quick Reference

| .NET Concept | Java / Spring Equivalent |
|---|---|
| `Program.cs` / `Startup.cs` | `FarmWorkersApplication.java` + `*Config.java` |
| `appsettings.json` | `application.properties` / `application.yml` |
| `[ApiController]` | `@RestController` |
| `[HttpGet]` / `[HttpPost]` | `@GetMapping` / `@PostMapping` |
| `[FromBody]` / `[FromRoute]` | `@RequestBody` / `@PathVariable` |
| `IActionResult` | `ResponseEntity<T>` |
| `Ok(result)` | `ResponseEntity.ok(result)` |
| `NotFound()` | throw `ResourceNotFoundException` → auto 404 |
| `[Authorize]` | `@PreAuthorize("isAuthenticated()")` |
| `ILogger<T>` | `@Slf4j` (Lombok) → `log.info(...)` |
| `AutoMapper` | `MapStruct` (`@Mapper`) |
| `[Required]` / `[MaxLength]` | `@NotBlank` / `@Size` |
| `IRepository<T>` | `extends CosmosRepository<T, String>` |
| `DbContext.SaveChangesAsync()` | `repository.save(entity)` |
| `LINQ .Where().Select()` | `.stream().filter().map()` |
| `string?` (nullable) | `Optional<T>` |
| `async Task<T>` | `CompletableFuture<T>` or reactive |
| `decimal` | `BigDecimal` |
| `Guid.NewGuid()` | `UUID.randomUUID()` |
| `DateTime.UtcNow` | `Instant.now()` |
| `[Table] / EF DbSet` | `@Container` + `CosmosRepository` |
| `xUnit + Moq` | `JUnit 5 + Mockito` |
| `Assert.Equal(a, b)` | `assertThat(a).isEqualTo(b)` |
| `NuGet packages` | `Maven dependencies (pom.xml)` |
| `dotnet build` | `mvn clean package` |
| `dotnet run` | `mvn spring-boot:run` |
| `dotnet test` | `mvn test` |

---

## 📁 Project Structure

```
src/main/java/com/farm/workers/
├── FarmWorkersApplication.java     # ≈ Program.cs
├── config/
│   ├── SecurityConfig.java         # ≈ AddAuthentication / AddAuthorization
│   ├── CosmosDbConfig.java         # ≈ AddDbContext<CosmosContext>
│   ├── WebClientConfig.java        # ≈ AddHttpClient()
│   └── OpenApiConfig.java          # ≈ AddSwaggerGen()
├── controller/
│   ├── WorkerController.java       # ≈ WorkersController.cs
│   └── WorkRecordController.java   # ≈ WorkRecordsController.cs
├── service/
│   ├── WorkerService.java          # Business logic layer
│   └── WorkRecordService.java      # Business logic layer
├── repository/
│   ├── WorkerRepository.java       # ≈ IRepository<Worker> + CosmosDB
│   └── WorkRecordRepository.java   # ≈ IRepository<WorkRecord>
├── model/
│   ├── Worker.java                 # ≈ Worker entity / domain model
│   └── WorkRecord.java             # ≈ WorkRecord entity
├── dto/
│   ├── WorkerDto.java              # ≈ WorkerDto record/class
│   └── WorkRecordDto.java          # ≈ WorkRecordDto record/class
├── azure/
│   ├── ServiceBusService.java      # ≈ Azure.Messaging.ServiceBus usage
│   ├── StorageQueueService.java    # ≈ Azure.Storage.Queues usage
│   ├── AzureFunctionService.java   # HTTP client calling Azure Functions
│   └── FarmWorkerFunctions.java    # The Azure Functions themselves (Java runtime)
├── exception/
│   ├── GlobalExceptionHandler.java # ≈ UseExceptionHandler / ProblemDetails
│   ├── ResourceNotFoundException.java
│   ├── ConflictException.java
│   └── BusinessRuleException.java
└── util/
    └── WorkerMapper.java           # ≈ AutoMapper Profile
```

---

## 🚀 Getting Started

### Prerequisites
- **Java 21** (≈ .NET 8 LTS)
  - Install via [SDKMAN](https://sdkman.io): `sdk install java 21-tem`
  - Or via [Homebrew](https://brew.sh): `brew install openjdk@21`
- **Maven 3.9+** (≈ dotnet CLI)
  - Install: `brew install maven` or download from maven.apache.org
- **Docker Desktop** (for local Azure emulators)
- **IntelliJ IDEA Community** (free, excellent Java IDE — better than VS Code for Java)
  - VS Code works too with the "Extension Pack for Java"

### 1. Start local Azure emulators
```bash
docker-compose up -d
# Starts: CosmosDB Emulator, Azurite (Storage), Service Bus Emulator
```

### 2. Set environment variables (≈ user-secrets in .NET)
```bash
# For development with real Azure:
export AZURE_TENANT_ID=your-tenant-id
export AZURE_CLIENT_ID=your-client-id
export AZURE_CLIENT_SECRET=your-client-secret

# Or use a .env file with the IDE run configuration
```

### 3. Run the application
```bash
# ≈ dotnet run
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Or build a JAR first (≈ dotnet publish):
mvn clean package
java -jar target/workers-api-1.0.0-SNAPSHOT.jar --spring.profiles.active=dev
```

### 4. Access Swagger UI
```
http://localhost:8080/swagger-ui.html
```

### 5. Run tests
```bash
# ≈ dotnet test
mvn test

# With coverage report
mvn test jacoco:report
# Open: target/site/jacoco/index.html
```

---

## 🔐 Azure Entra ID Setup

### Register an App in Entra ID
1. Go to **Azure Portal → Entra ID → App Registrations → New Registration**
2. Set **Redirect URI**: `http://localhost:8080` (for dev)
3. Under **Expose an API**, add scopes:
   - `workers.read`
   - `workers.write`
   - `workers.admin`
   - `records.write`
   - `records.approve`
4. Under **Certificates & Secrets**, create a client secret
5. Note down: **Tenant ID**, **Client ID**, **Client Secret**

### Get a test token (≈ using Postman with OAuth2 in .NET)
```bash
curl -X POST \
  "https://login.microsoftonline.com/{tenant-id}/oauth2/v2.0/token" \
  -d "client_id={client-id}" \
  -d "client_secret={client-secret}" \
  -d "grant_type=client_credentials" \
  -d "scope=api://{client-id}/.default"
```

Use the returned `access_token` as `Bearer {token}` in requests.

---

## 📡 API Endpoints

### Workers
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/v1/workers` | List all workers |
| GET | `/api/v1/workers?search=Ion` | Search workers |
| GET | `/api/v1/workers?status=ACTIVE` | Filter by status |
| GET | `/api/v1/workers/{id}` | Get worker by ID |
| POST | `/api/v1/workers` | Register new worker |
| PUT | `/api/v1/workers/{id}` | Update worker |
| DELETE | `/api/v1/workers/{id}` | Deactivate worker |

### Work Records
| Method | URL | Description |
|--------|-----|-------------|
| GET | `/api/v1/work-records/worker/{id}` | All records for worker |
| GET | `/api/v1/work-records/worker/{id}/season/{season}` | Records by season |
| GET | `/api/v1/work-records/worker/{id}/season/{season}/summary` | Season summary |
| POST | `/api/v1/work-records` | Log a work shift |
| PATCH | `/api/v1/work-records/{id}/approve` | Approve a record |

---

## ☁️ Azure Integration Architecture

```
Spring Boot API
    │
    ├── CosmosDB (NoSQL) ──────────── stores Workers + WorkRecords
    │     └── Change Feed ──────────→ Azure Function: OnWorkerChanged
    │
    ├── Service Bus Queue ─────────→ Azure Function: OnWorkerCreated
    │     worker-events                (background check, onboarding)
    │
    ├── Service Bus Topic ─────────→ Azure Function: GeneratePayslipOnPayroll
    │     payroll-events               (PDF generation, email)
    │
    ├── Storage Queue ─────────────→ Azure Function: ProcessWorkRecordCreated
    │     work-records-created          (validation, supervisor notification)
    │
    └── HTTP calls ────────────────→ Azure Function: ComputeSeasonPayroll
          (on-demand)                  (compute-heavy aggregation)
```

---

## 🧰 Key Java Concepts for .NET Developers

### Dependency Injection
Spring uses **annotation-based DI** vs .NET's fluent registration:
```java
// .NET: services.AddScoped<IWorkerService, WorkerService>();
// Java: Just add @Service - Spring finds it automatically via @ComponentScan

@Service
public class WorkerService {
    private final WorkerRepository repo;  // final = readonly

    // Spring sees ONE constructor → auto-injects WorkerRepository
    // @RequiredArgsConstructor (Lombok) generates this constructor
    public WorkerService(WorkerRepository repo) {
        this.repo = repo;
    }
}
```

### Streams vs LINQ
```java
// C# LINQ:
workers.Where(w => w.Status == Active)
       .Select(w => mapper.Map<WorkerSummary>(w))
       .ToList();

// Java Streams:
workers.stream()
       .filter(w -> w.getStatus() == ACTIVE)
       .map(workerMapper::toSummary)
       .collect(Collectors.toList());
```

### Optional vs Nullable
```java
// C# nullable:
Worker? worker = repo.FindById(id);
if (worker is null) throw new NotFoundException();

// Java Optional:
Worker worker = repo.findById(id)
    .orElseThrow(() -> new ResourceNotFoundException("Worker", "id", id));
```

### Records / Lombok
```java
// C# record:
public record CreateWorkerRequest(string FirstName, string LastName);

// Java with Lombok (generates all boilerplate):
@Data @Builder @NoArgsConstructor @AllArgsConstructor
public class CreateRequest {
    private String firstName;
    private String lastName;
}
// Usage: CreateRequest.builder().firstName("Ion").build()
```
