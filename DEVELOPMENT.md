# 📖 Development Guide

This guide covers everything you need to know for local development, testing, debugging, and deployment of the spring-boot-repo projects.

---

## 📋 Table of Contents

- [Local Development Setup](#local-development-setup)
- [Project Structure](#project-structure)
- [Building Projects](#building-projects)
- [Running Projects](#running-projects)
- [Testing](#testing)
- [Debugging](#debugging)
- [Database Setup](#database-setup)
- [Docker & Containers](#docker--containers)
- [Code Generation](#code-generation)
- [Troubleshooting](#troubleshooting)
- [Performance Optimization](#performance-optimization)
- [Security Best Practices](#security-best-practices)

---

## 🚀 Local Development Setup

### One-Time Setup

#### Windows 11 + IntelliJ IDEA

```powershell
# 1. Install Java 21
choco install eclipse-temurin21 -y

# 2. Install Maven
choco install maven -y

# 3. Install Git
choco install git -y

# 4. Install Docker (optional)
choco install docker-desktop -y

# 5. Install IntelliJ
choco install jetbrains-toolbox -y
# Then install IntelliJ Community Edition from Toolbox

# 6. Clone repository
git clone https://github.com/yourusername/spring-boot-repo.git
cd spring-boot-repo

# 7. Open in IntelliJ
idea .

# 8. Configure IDE
# File → Project Structure (Ctrl+Alt+Shift+S)
# Set Project SDK to Java 21
```

#### Linux Ubuntu + VS Code

```bash
# 1. Install Java 21
sudo apt update
sudo apt install -y temurin-21-jdk

# 2. Install Maven
sudo apt install -y maven

# 3. Install Git
sudo apt install -y git

# 4. Install Docker (optional)
curl -fsSL https://get.docker.com -o get-docker.sh
sudo sh get-docker.sh

# 5. Install VS Code
sudo snap install --classic code

# 6. Clone repository
git clone https://github.com/yourusername/spring-boot-repo.git
cd spring-boot-repo

# 7. Open in VS Code
code .

# 8. Install Extensions
code --install-extension vscjava.extension-pack-for-java

# 9. Configure Java Runtime
# Ctrl+Shift+P → "Java: Configure Runtime" → Select Java 21
```

#### Cross-Platform (SDKMAN - Linux/Mac/WSL)

```bash
# 1. Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh

# 2. Install all required versions
cd spring-boot-repo
sdk env install

# 3. Verify installation
java -version    # 21.0.3
mvn --version    # 3.9.6
gradle --version # 8.8.1
```

### Environment Configuration

Create `.env.local` for local settings:

```bash
# Copy from example
cp .env.example .env.local

# Edit with your settings
AZURE_SUBSCRIPTION_ID=your-subscription-id
AZURE_TENANT_ID=your-tenant-id
AZURE_CLIENT_ID=your-client-id
COSMOS_CONNECTION_STRING=your-cosmos-connection
SERVICEBUS_CONNECTION_STRING=your-servicebus-connection
```

---

## 📁 Project Structure

```
spring-boot-repo/
├── azure-java-platform/          # Enterprise reference (Maven multi-module)
│   ├── pom.xml                   # Parent POM with Java 21, Spring Boot 3.3.4
│   ├── shared/                   # Shared DTOs and models
│   ├── webapi/                   # REST API service
│   ├── functions/                # Azure Functions (serverless)
│   ├── infra/                    # Terraform infrastructure
│   ├── local/                    # Docker Compose for local dev
│   └── docs/                     # Architecture and deployment docs
│
├── farm-workers-api/             # Learning project (Maven single)
│   ├── pom.xml                   # Java 21, Spring Boot 3.3.4
│   ├── src/main/java/            # Application code
│   ├── src/test/java/            # Unit tests
│   ├── azure-functions/          # Azure Functions
│   └── docker-compose.yml        # Local services
│
├── spring-academy-intro/         # Beginner tutorial (Gradle)
│   ├── build.gradle              # Gradle build, Java 21, Spring Boot 3.3.4
│   ├── src/main/java/            # Example code
│   └── src/test/java/            # Tests
│
├── .editorconfig                 # IDE formatting rules
├── .sdkmanrc                     # SDKMAN version management
├── .gitignore                    # Git ignore rules (comprehensive)
├── .pre-commit-config.yaml       # Pre-commit hooks
└── docs/                         # Documentation
```

---

## 🔨 Building Projects

### Maven Projects

```bash
# Build azure-java-platform
cd azure-java-platform/azure-java-platform
mvn clean install                 # Full build with tests

# Build specific module
mvn -pl shared clean install      # Build only shared module
mvn -pl webapi clean install      # Build only webapi

# Build farm-workers-api
cd farm-workers-api/farm-workers-api
mvn clean package                 # Package for deployment

# Build without tests
mvn clean install -DskipTests

# Build with specific profile
mvn clean install -P production
```

### Gradle Project

```bash
# Build spring-academy-intro
cd spring-academy-intro
./gradlew build                   # Full build

# Build without tests
./gradlew build -x test

# Build specific task
./gradlew compileJava
./gradlew compileTestJava
```

### Build Artifacts

- **Maven**: `target/*.jar` or `target/*.war`
- **Gradle**: `build/libs/*.jar`

---

## ▶️ Running Projects

### Azure Java Platform (Web API)

```bash
cd azure-java-platform/azure-java-platform

# Start local Azure services (optional)
docker compose -f local/docker-compose.yml up -d

# Run REST API
mvn -pl webapi spring-boot:run

# Access: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
# Health: http://localhost:8080/actuator/health
```

### Azure Functions (Local)

```bash
cd azure-java-platform/azure-java-platform

# Install Azure Functions Core Tools (if not installed)
choco install azure-functions-core-tools-3 -y  # Windows
sudo apt install azure-functions-core-tools    # Linux

# Run Azure Functions
mvn -pl functions azure-functions:run

# Access: http://localhost:7071
# Function routes defined in src/main/java/functions/
```

### Farm Workers API

```bash
cd farm-workers-api/farm-workers-api

# Start local services
docker-compose up -d

# Run with default profile
mvn spring-boot:run

# Run with specific profile
mvn spring-boot:run -Dspring-boot.run.profiles=dev
mvn spring-boot:run -Dspring-boot.run.profiles=test

# Access: http://localhost:8080
# Swagger UI: http://localhost:8080/swagger-ui.html
```

### Spring Academy Intro

```bash
cd spring-academy-intro

# Run with Gradle
./gradlew bootRun

# Or build and run JAR
./gradlew build
java -jar build/libs/demo-0.0.1-SNAPSHOT.jar

# Access: http://localhost:8080
```

### Stopping Services

```bash
# Stop Spring Boot apps: Ctrl+C in terminal

# Stop Docker services
docker compose down                # From project directory
docker compose -f local/docker-compose.yml down  # From azure-java-platform
```

---

## ✅ Testing

### Unit Tests

```bash
# Run all tests
cd azure-java-platform/azure-java-platform
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#testGetUserById

# Gradle
cd spring-academy-intro
./gradlew test
```

### Integration Tests

```bash
# Run integration tests (marked with @SpringBootTest)
mvn verify

# Run with specific groups
mvn test -Dgroups=integration
```

### Code Coverage

```bash
# Generate JaCoCo coverage report
mvn test jacoco:report

# View report
open target/site/jacoco/index.html      # macOS
xdg-open target/site/jacoco/index.html  # Linux

# Gradle
./gradlew jacocoTestReport
```

### Test Best Practices

- **Unit Tests**: Test single units in isolation
- **Integration Tests**: Test multiple components together
- **Test Data**: Use builders or factories for test objects
- **Mocking**: Mock external dependencies (databases, APIs)
- **Assertions**: Use meaningful assertion messages

Example:

```java
@SpringBootTest
class UserServiceIntegrationTest {
    
    @Autowired
    private UserService userService;
    
    @Autowired
    private UserRepository userRepository;
    
    @Test
    void testCreateUserAndRetrieve() {
        // Arrange
        User user = User.builder()
            .name("John Doe")
            .email("john@example.com")
            .build();
        
        // Act
        User saved = userService.createUser(user);
        User retrieved = userService.getUserById(saved.getId());
        
        // Assert
        assertNotNull(saved.getId());
        assertEquals(user.getName(), retrieved.getName());
    }
}
```

---

## 🐛 Debugging

### IntelliJ IDEA

```
1. Set breakpoint: Click left margin next to line number
2. Run in debug mode: Shift+F9 or Run → Debug
3. Step through: F10 (step over), F11 (step into)
4. Inspect variables: Hover over variable
5. Console: View/Hide → Debug Console
6. Evaluate expression: Alt+F9
```

### VS Code

```
1. Set breakpoint: Click left margin
2. Launch debugger: F5 or Run → Start Debugging
3. Step through: F10 (step over), F11 (step into)
4. Inspect variables: Hover or Debug Console
5. Debug Terminal: Shows all output
```

### Remote Debugging

```bash
# Start Java app with debug port
java -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=5005 \
     -jar application.jar

# Configure IDE to connect to port 5005
```

### Application Logging

```bash
# Enable debug logging
LOGGING_LEVEL_ROOT=DEBUG mvn spring-boot:run

# Or in application.properties
logging.level.root=DEBUG
logging.level.com.example=DEBUG
```

---

## 🗄️ Database Setup

### Local Cosmos DB Emulator

```bash
# Start emulator (requires Docker)
cd azure-java-platform/azure-java-platform
docker compose -f local/docker-compose.yml up cosmosdb

# Connection string (auto-configured)
AccountEndpoint=https://localhost:8081/
AccountKey=C2y6yDjf5/R+ob0N8A7Cgv30VRDJIWEHLM+4QDU5DE2nQ9nDuVo+tYUf/Vam/Cc+7Uh0B0/NeWZXIGojo/byA==

# Access Cosmos Emulator: https://localhost:8081/_explorer/
```

### H2 Database (In-Memory)

```bash
# Configured automatically in spring-academy-intro

# Access H2 Console: http://localhost:8080/h2-console
# JDBC URL: jdbc:h2:mem:testdb
# User: sa
# Password: (leave blank)
```

### SQL Server (Azure)

```bash
# Connection properties in application.properties
spring.datasource.url=jdbc:sqlserver://SERVER:1433;database=DATABASE
spring.datasource.username=USER
spring.datasource.password=PASSWORD
```

---

## 🐳 Docker & Containers

### Docker Compose

```bash
# Start all services
cd azure-java-platform/azure-java-platform
docker compose -f local/docker-compose.yml up -d

# List running services
docker compose ps

# View logs
docker compose logs -f SERVICE_NAME

# Stop all services
docker compose down

# Remove volumes
docker compose down -v
```

### Building Docker Images

```bash
# Build Docker image
docker build -t spring-boot-repo:latest .

# Run container
docker run -p 8080:8080 spring-boot-repo:latest

# Interactive terminal
docker run -it spring-boot-repo:latest bash
```

---

## 🔄 Code Generation

### Spring Boot CLI

```bash
# Create new Spring Boot project
spring boot new --from=https://... my-app

# Run project
cd my-app
./mvnw spring-boot:run
```

### Maven Archetype

```bash
# Generate project from archetype
mvn archetype:generate \
  -DgroupId=com.example \
  -DartifactId=my-app \
  -Dpackage=com.example \
  -Dversion=1.0.0
```

### Entity Generation

```bash
# Generate entities from database (JPA)
mvn jpa:generate-entities -Ddb.url=... -Ddb.user=... -Ddb.password=...
```

---

## 🔧 Troubleshooting

### Common Issues

| Issue | Cause | Solution |
|-------|-------|----------|
| **Java version mismatch** | Java 25/17 instead of 21 | Install Java 21: `choco install eclipse-temurin21` |
| **Maven not found** | Maven not in PATH | Install Maven: `choco install maven` |
| **Port 8080 in use** | Another app using port | Kill process: `lsof -i :8080` or change port |
| **Build fails** | Stale cache | Clear: `mvn clean` or `./gradlew clean` |
| **Tests fail** | Missing dependencies | Run: `mvn install` or `./gradlew build` |
| **Docker won't start** | Docker daemon not running | Start Docker Desktop (Windows/Mac) or daemon (Linux) |
| **EditorConfig not working** | Plugin disabled | Enable in IDE settings |

### Debug Output

```bash
# Verbose Maven output
mvn -X clean install

# Gradle debug output
./gradlew build --debug

# Show dependency tree
mvn dependency:tree
```

---

## ⚡ Performance Optimization

### Build Performance

```bash
# Parallel builds
mvn -T 1C clean install        # 1 thread per core

# Skip slow operations
mvn clean install -DskipTests  # Skip tests
mvn clean install -Dspotless.check.skip  # Skip formatting check
```

### Runtime Performance

```bash
# Increase JVM heap
JAVA_OPTS="-Xmx2g -Xms1g" mvn spring-boot:run

# Run with JVM profiler
java -XX:+UnlockCommercialFeatures \
     -XX:+FlightRecorder \
     -jar application.jar
```

### Database Performance

```bash
# Connection pooling (configured in Spring)
spring.datasource.hikari.maximum-pool-size=20

# Query optimization: Use indexes and proper queries
# Monitor with: http://localhost:8080/actuator/metrics
```

---

## 🔒 Security Best Practices

### Secrets Management

```bash
# Use environment variables (never hardcode secrets)
export DATABASE_PASSWORD=your_password
export API_KEY=your_api_key

# Or use .env.local (excluded from git)
cat .env.local
# COSMOS_CONNECTION_STRING=...
# SERVICEBUS_CONNECTION_STRING=...
```

### Dependency Security

```bash
# Check for vulnerabilities
mvn org.owasp:dependency-check-maven:check

# Update dependencies
mvn versions:update-properties

# Review security advisories
# https://www.cvedetails.com/
```

### Authentication & Authorization

```java
// Use Spring Security for authentication
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {
        return http
            .oauth2Login()
            .and()
            .authorizeRequests()
            .anyRequest().authenticated()
            .and()
            .build();
    }
}
```

### HTTPS & TLS

```bash
# Use HTTPS in production
server.ssl.key-store=classpath:keystore.p12
server.ssl.key-store-password=password
server.ssl.key-store-type=PKCS12
```

---

## 📚 Additional Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Guide](https://maven.apache.org/guides/)
- [Gradle Manual](https://docs.gradle.org/current/userguide/)
- [Azure SDK for Java](https://learn.microsoft.com/azure/developer/java/)
- [Java Security Best Practices](https://owasp.org/www-project-top-ten/)

---

## 🆘 Getting Help

1. Check this guide for solutions
2. Search GitHub issues
3. Review project-specific README
4. Open GitHub discussion
5. Check Azure documentation

---

**Last Updated**: July 31, 2026

