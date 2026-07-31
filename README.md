# Spring Boot Repository — Multi-Project Azure Integration Hub

A comprehensive collection of **three modern Java/Spring Boot projects** showcasing cloud-native application development with Azure integration, microservices patterns, and enterprise best practices.

---

## 📦 Projects Overview

### 1. **azure-java-platform** ⭐ Enterprise Reference Architecture
**Purpose**: Full-stack Azure cloud platform reference implementation for enterprise Java applications.

**Technology Stack**:
- **Java 17** | **Spring Boot 3.3.4** | **Maven** (multi-module)
- **Web API**: Spring Boot REST service with OpenAPI/Swagger
- **Serverless**: Azure Functions (Java v4) with HTTP and Kafka triggers
- **Data**: Azure SQL Server + Cosmos DB (NoSQL) with Flyway migrations
- **Messaging**: Event Hubs with Kafka compatibility
- **Auth**: Entra ID (Azure AD) JWT bearer tokens + Managed Identity
- **Observability**: Application Insights with Java agent
- **IaC**: Terraform modules (App Service, Function App, SQL, Cosmos, Key Vault, Event Hubs)
- **CI/CD**: Azure DevOps YAML pipeline
- **Local Dev**: Docker Compose (SQL Server, Redpanda Kafka emulator, Cosmos emulator, Azurite)

**Key Features**:
- Multi-module Maven project (shared, webapi, functions)
- End-to-end authentication with Entra ID
- Long-running job processing (202 Accepted + polling pattern)
- Event-driven serverless functions
- Fully documented (Architecture, local setup, deployment guides)
- Production-ready infrastructure as code

**Directory**: `./azure-java-platform/azure-java-platform/`  
**Quick Start**: See [`azure-java-platform/docs/RUN_LOCAL.md`](./azure-java-platform/azure-java-platform/docs/RUN_LOCAL.md)

---

### 2. **farm-workers-api** 🌾 .NET Developer Onboarding
**Purpose**: Learning project designed to teach .NET developers Java/Spring Boot equivalents through a real-world domain (farm seasonal worker management).

**Technology Stack**:
- **Java 21** (LTS) | **Spring Boot 3.2.3** | **Maven**
- **Web API**: RESTful endpoints for worker and work record management
- **Data**: Cosmos DB (NoSQL) with Spring Data Cosmos repositories
- **Messaging**: Service Bus Queues & Topics, Azure Storage Queues
- **Auth**: Entra ID OAuth 2.0 with Spring Security
- **Mapping**: MapStruct for entity-to-DTO transformations
- **Testing**: JUnit 5 + Mockito with code coverage (Jacoco)
- **Documentation**: OpenAPI/Swagger UI
- **Local Dev**: Docker Compose (Cosmos Emulator, Azurite, Service Bus Emulator)

**Key Features**:
- Comprehensive **".NET → Java Quick Reference"** in README (54 concepts mapped)
- Clean architecture: Controller → Service → Repository layers
- Exception handling with global handler
- Search and filtering capabilities
- Work records with season-based aggregations
- Detailed Entra ID setup guide
- Multiple Azure integration patterns (queues, topics, change feeds)

**Directory**: `./farm-workers-api/farm-workers-api/`  
**Quick Start**: See [`farm-workers-api/README.md`](./farm-workers-api/farm-workers-api/README.md#-getting-started)

---

### 3. **spring-academy-intro** 📚 Learning Project
**Purpose**: Beginner-level Spring Boot tutorial project covering fundamentals and best practices.

**Technology Stack**:
- **Java 25** | **Spring Boot 4.0.6** | **Gradle**
- **Framework**: Spring Web MVC
- **Example**: CashCard REST service (CRUD operations)
- **Testing**: JUnit 5 with Spring Boot Test

**Key Features**:
- Clean, beginner-friendly codebase
- Demonstrates REST API fundamentals
- Gradle-based build (alternative to Maven)
- Unit and integration tests

**Directory**: `./spring-academy-intro/`  
**Quick Start**: See [`spring-academy-intro/HELP.md`](./spring-academy-intro/HELP.md)

---

## 🎯 When to Use Each Project

| Use Case | Project | Why |
|----------|---------|-----|
| **Learn Java/Spring for first time** | `spring-academy-intro` | Simplest, minimal dependencies, CRUD fundamentals |
| **.NET developer → Java transition** | `farm-workers-api` | Domain-driven, extensive .NET↔Java mappings, real patterns |
| **Production architecture reference** | `azure-java-platform` | Enterprise patterns, multi-tier, IaC, CI/CD, cloud-native |
| **Local microservices development** | `azure-java-platform` or `farm-workers-api` | Full Docker Compose stacks included |
| **Azure serverless example** | `azure-java-platform` | Azure Functions, Event Hubs, Service Bus patterns |
| **REST API design patterns** | `farm-workers-api` | Clean RESTful structure, OpenAPI/Swagger |

---

## 🚀 Getting Started

### Prerequisites (All Projects)
- **Java Development Kit (JDK)**:
  - `azure-java-platform`: **Java 17** (LTS)
  - `farm-workers-api`: **Java 21** (LTS)
  - `spring-academy-intro`: **Java 25**
  - Download: [eclipse-temurin.net](https://adoptium.net) or [SDKMAN](https://sdkman.io)

- **Build Tools**:
  - `azure-java-platform`: **Maven 3.9+**
  - `farm-workers-api`: **Maven 3.9+**
  - `spring-academy-intro`: **Gradle** (included via wrapper)

- **Container Runtime** (for local Azure services):
  - **Docker Desktop** (Windows/Mac) or **Docker Engine** (Linux)

- **IDE**:
  - **IntelliJ IDEA Community** (recommended, free, excellent Java support)
  - **VS Code** with "Extension Pack for Java"

### Quick Clone & Explore
```powershell
# Clone the repository
git clone https://github.com/yourusername/spring-boot-repo.git
cd spring-boot-repo

# Navigate to a project
cd azure-java-platform/azure-java-platform
# or
cd farm-workers-api/farm-workers-api
# or
cd spring-academy-intro
```

### Project-Specific Setup
- **azure-java-platform**: 
  ```powershell
  cd azure-java-platform/azure-java-platform
  docker compose -f local/docker-compose.yml up -d
  mvn clean install
  # See docs/RUN_LOCAL.md for detailed steps
  ```

- **farm-workers-api**:
  ```powershell
  cd farm-workers-api/farm-workers-api
  docker-compose up -d
  mvn spring-boot:run -Dspring-boot.run.profiles=dev
  # Access: http://localhost:8080/swagger-ui.html
  ```

- **spring-academy-intro**:
  ```powershell
  cd spring-academy-intro
  ./gradlew bootRun
  # or
  ./gradlew build && java -jar build/libs/*.jar
  ```

---

## 🏗️ Architecture & Patterns

### Multi-Module Maven (azure-java-platform)
```
azure-java-platform/
├── shared/          # DTOs, events, shared models
├── webapi/          # Spring Boot REST API
├── functions/       # Azure Functions (serverless)
└── infra/           # Terraform: complete cloud infrastructure
```

### Layered Architecture (farm-workers-api)
```
src/main/java/com/farm/workers/
├── controller/      # HTTP endpoints
├── service/         # Business logic
├── repository/      # Data access (Cosmos DB)
├── model/           # Domain entities
├── dto/             # Data transfer objects
├── azure/           # Azure SDK integrations
├── exception/       # Error handling
└── config/          # Spring configuration
```

### Key Cloud Patterns
1. **Managed Identity**: Service-to-service auth without secrets
2. **Event-Driven**: Azure Service Bus, Event Hubs for async processing
3. **Serverless**: Azure Functions for bursty, short-lived workloads
4. **Infrastructure as Code**: Terraform for repeatable deployments
5. **Observability**: Application Insights for logs, traces, metrics

---

## 🔐 Security & Authentication

### Entra ID (Azure AD) Integration
All projects support OAuth 2.0 via **Microsoft Entra ID**:
- **Bearer token validation** on REST endpoints
- **Managed Identity** for Azure-to-Azure communication (no secrets in code)
- **Scopes/Permissions** for fine-grained access control
- Setup guides included in each project's README

### Local Development
- Use environment variables for secrets (`.env` files)
- Docker Compose includes emulated Azure services (no real credentials needed)
- Example: `AZURE_CLIENT_ID`, `AZURE_TENANT_ID`, etc.

---

## 📖 Key Technologies & Versions

| Component | azure-java-platform | farm-workers-api | spring-academy-intro |
|-----------|---------------------|------------------|----------------------|
| **Java** | 17 (LTS) | 21 (LTS) | 25 |
| **Spring Boot** | 3.3.4 | 3.2.3 | 4.0.6 |
| **Build Tool** | Maven | Maven | Gradle |
| **Azure SDK** | Various (Functions, Cosmos, Event Hubs) | Azure SDK 5.10.0 | None |
| **Database** | SQL Server + Cosmos DB | Cosmos DB | None |
| **Messaging** | Event Hubs | Service Bus + Storage Queues | None |
| **Testing** | JUnit, Maven Surefire | JUnit 5, Mockito | JUnit 5 |
| **Documentation** | OpenAPI/Swagger | OpenAPI/Swagger | Basic |

---

## 📚 Learning Path

**Complete Beginner** → **Intermediate** → **Advanced/Production**

```
1. Start: spring-academy-intro
   └─ Learn: REST APIs, Spring fundamentals, basic testing
   └─ Tech: Java 21 LTS, Spring Boot 4.0.6, Gradle 8.8.1

2. Intermediate: farm-workers-api  
   └─ Learn: Layered architecture, Cosmos DB, Azure Services, .NET↔Java mapping
   └─ Tech: Java 21 LTS, Spring Boot 3.3.4, Maven 3.13.0

3. Advanced: azure-java-platform
   └─ Learn: Microservices, serverless, IaC, observability, enterprise patterns
   └─ Tech: Java 21 LTS, Spring Boot 3.3.4, Maven 3.13.0
```

> **Standardization**: All projects use **Java 21 LTS** (latest stable), **Spring Boot 3.3.4+**, and latest build tools

---

## 🛠️ Common Tasks

### Build All Projects
```powershell
# azure-java-platform
cd azure-java-platform/azure-java-platform && mvn clean install

# farm-workers-api
cd farm-workers-api/farm-workers-api && mvn clean package

# spring-academy-intro
cd spring-academy-intro && ./gradlew build
```

### Run Tests
```powershell
# Maven projects
mvn test
mvn test -Dtest=ClassName  # Specific test class

# Gradle project
./gradlew test
./gradlew test --tests "com.example.*CashCard*"
```

### Generate Code Coverage
```powershell
# Maven with Jacoco
mvn test jacoco:report
# Open: target/site/jacoco/index.html

# Gradle with Jacoco
./gradlew jacocoTestReport
```

### Deploy to Azure
See **azure-java-platform**: [`docs/DEPLOY.md`](./azure-java-platform/azure-java-platform/docs/DEPLOY.md)

---

## 📋 Repository Structure

```
spring-boot-repo/
├── azure-java-platform/          # Enterprise reference platform
│   ├── azure-java-platform/
│   │   ├── webapi/               # Spring Boot Web API
│   │   ├── functions/            # Azure Functions (Java v4)
│   │   ├── shared/               # Shared models & DTOs
│   │   ├── infra/                # Terraform infrastructure
│   │   ├── local/                # Docker Compose local dev
│   │   ├── scripts/              # Setup scripts
│   │   ├── pipelines/            # Azure DevOps CI/CD
│   │   └── docs/                 # Architecture, deployment guides
│   └── [pom.xml]                 # Parent Maven POM
│
├── farm-workers-api/             # .NET developer learning project
│   ├── farm-workers-api/
│   │   ├── src/main/java/        # Spring Boot API code
│   │   ├── src/test/java/        # Unit & integration tests
│   │   ├── azure-functions/      # Azure Functions
│   │   ├── docker-compose.yml    # Local services
│   │   └── README.md             # Detailed guide (.NET↔Java mappings)
│   └── [pom.xml]                 # Maven POM
│
├── spring-academy-intro/         # Beginner learning project
│   ├── src/main/java/            # Spring Boot example code
│   ├── src/test/java/            # JUnit tests
│   ├── build.gradle              # Gradle build file
│   ├── HELP.md                   # Getting started
│   └── gradlew / gradlew.bat     # Gradle wrapper
│
└── README.md                      # This file
```

---

## 🤝 Contributing

All projects follow standard Java/Spring Boot conventions:
- **Naming**: PascalCase for classes, camelCase for variables
- **Structure**: Controller → Service → Repository
- **Docs**: JavaDoc for public APIs
- **Testing**: JUnit 5, @ParameterizedTest for multiple cases
- **Formatting**: Use IDE auto-formatter or Maven Checkstyle

---

## 📖 Additional Resources

### Spring Boot Official Docs
- [Spring Boot Reference](https://spring.io/projects/spring-boot)
- [Spring Framework Guides](https://spring.io/guides)

### Azure + Java
- [Azure SDK for Java](https://learn.microsoft.com/azure/developer/java/)
- [Spring Cloud Azure](https://spring.io/projects/spring-cloud-azure)
- [Azure Functions Java Developer Guide](https://learn.microsoft.com/azure/azure-functions/functions-reference-java)

### Learning Resources
- **Java Basics**: [Oracle Java Tutorials](https://docs.oracle.com/javase/tutorial/)
- **Spring Boot**: [Spring Academy](https://spring.academy)
- **.NET → Java**: See `farm-workers-api/README.md` for comprehensive mapping

---

## ⚖️ License

MIT — Sample code. Review and harden before production use.

---

## 📞 Questions or Issues?

- **azure-java-platform**: See [`docs/ARCHITECTURE.md`](./azure-java-platform/azure-java-platform/docs/ARCHITECTURE.md)
- **farm-workers-api**: See README section "🧰 Key Java Concepts for .NET Developers"
- **spring-academy-intro**: See [`HELP.md`](./spring-academy-intro/HELP.md)

---

**Last Updated**: July 31, 2026
