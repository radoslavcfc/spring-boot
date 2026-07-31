# 🚀 SETUP QUICK START GUIDE

## Windows 11 + IntelliJ IDEA

### 1️⃣ Install Java 21 LTS
```powershell
# Using Chocolatey (easiest)
choco install eclipse-temurin21 -y

# Verify
java -version
# Expected: openjdk 21.0.3
```

### 2️⃣ Install Maven
```powershell
choco install maven -y
mvn --version
```

### 3️⃣ Install Git
```powershell
choco install git -y
```

### 4️⃣ Clone Repository
```powershell
git clone https://github.com/yourusername/spring-boot-repo.git
cd spring-boot-repo
```

### 5️⃣ Install & Configure IntelliJ
```powershell
choco install jetbrains-toolbox -y
# Open Toolbox → Install IntelliJ Community Edition
```

Open project:
- IntelliJ → File → Open → select `C:\Java\spring-boot-repo`
- Wait for indexing
- File → Project Structure (Ctrl+Alt+Shift+S)
- Set SDK to Java 21

### 6️⃣ Build All Projects
Open IntelliJ Terminal (Alt+F12):
```powershell
# Build azure-java-platform
cd azure-java-platform\azure-java-platform
mvn clean install

# Build farm-workers-api
cd ..\..\farm-workers-api\farm-workers-api
mvn clean package

# Build spring-academy-intro
cd ..\..\spring-academy-intro
.\gradlew build
```

### 7️⃣ Run Projects
```powershell
# azure-java-platform
cd azure-java-platform\azure-java-platform
docker compose -f local\docker-compose.yml up -d
mvn -pl webapi spring-boot:run
# http://localhost:8080

# farm-workers-api
cd farm-workers-api\farm-workers-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# http://localhost:8080/swagger-ui.html

# spring-academy-intro
cd spring-academy-intro
.\gradlew bootRun
# http://localhost:8080
```

---

## 🐧 Linux Ubuntu + VS Code

### 1️⃣ Install Java 21 LTS
```bash
sudo apt update
sudo apt install -y temurin-21-jdk

# Verify
java -version
# Expected: openjdk 21.0.3
```

### 2️⃣ Install Maven
```bash
sudo apt install -y maven
mvn --version
```

### 3️⃣ Install Git
```bash
sudo apt install -y git
```

### 4️⃣ Clone Repository
```bash
git clone https://github.com/yourusername/spring-boot-repo.git
cd spring-boot-repo
```

### 5️⃣ Install VS Code
```bash
sudo snap install --classic code
# Or: sudo apt install code
```

Install Java Extensions:
```bash
code --install-extension vscjava.extension-pack-for-java
code --install-extension vscjava.vscode-maven
code --install-extension vscjava.vscode-gradle
```

### 6️⃣ Open & Configure Project
```bash
code .
```

In VS Code:
- Ctrl+Shift+P → "Java: Configure Runtime"
- Select Java 21

### 7️⃣ Build All Projects
Open Terminal (Ctrl+`):
```bash
# Build azure-java-platform
cd azure-java-platform/azure-java-platform
mvn clean install

# Build farm-workers-api
cd ../../farm-workers-api/farm-workers-api
mvn clean package

# Build spring-academy-intro
cd ../../spring-academy-intro
chmod +x gradlew  # First time only
./gradlew build
```

### 8️⃣ Run Projects
```bash
# azure-java-platform
cd azure-java-platform/azure-java-platform
docker compose -f local/docker-compose.yml up -d
mvn -pl webapi spring-boot:run
# http://localhost:8080

# farm-workers-api
cd farm-workers-api/farm-workers-api
mvn spring-boot:run -Dspring-boot.run.profiles=dev
# http://localhost:8080/swagger-ui.html

# spring-academy-intro
cd spring-academy-intro
./gradlew bootRun
# http://localhost:8080
```

---

## 🔄 SDKMAN Setup (Linux/Mac/WSL)

### Install SDKMAN
```bash
curl -s "https://get.sdkman.io" | bash
source ~/.sdkman/bin/sdkman-init.sh
```

### Auto-Install All Versions
```bash
cd spring-boot-repo
sdk env install  # Installs Java 21.0.3, Maven 3.9.6, Gradle 8.8.1

# Verify
java -version
mvn --version
gradle --version

# When done, clear environment
sdk env clear
```

---

## ✅ Verification Checklist

After setup, run:

### Windows PowerShell
```powershell
java -version
mvn --version
git --version
cd spring-boot-repo
cd azure-java-platform\azure-java-platform
mvn clean compile
```

### Linux Bash
```bash
java -version
mvn --version
git --version
cd spring-boot-repo
cd azure-java-platform/azure-java-platform
mvn clean compile
```

### Expected Output
```
openjdk 21.0.3 2024-10-15
Apache Maven 3.9.6
git version 2.x.x
```

---

## 📋 Maven Commands (Quick Reference)

```bash
# Build
mvn clean install
mvn clean package
mvn clean compile

# Run
mvn spring-boot:run
mvn spring-boot:run -Dspring-boot.run.profiles=dev

# Test
mvn test
mvn test -Dtest=ClassName

# Coverage
mvn test jacoco:report

# Multi-module specific
mvn -pl modulename clean install
mvn -pl webapi spring-boot:run
```

## 📋 Gradle Commands (Quick Reference)

```bash
# Build
./gradlew build
./gradlew clean assemble
./gradlew compile

# Run
./gradlew bootRun

# Test
./gradlew test

# Coverage
./gradlew jacocoTestReport
```

---

## 🐛 Common Issues & Fixes

| Issue | Fix |
|-------|-----|
| **mvn command not found (Windows)** | Add Maven to PATH or install via Chocolatey |
| **gradle permission denied (Linux)** | `chmod +x gradlew` |
| **Port 8080 in use** | Kill process: `lsof -i :8080` or use different port |
| **Java version wrong** | Check PATH, or use SDKMAN: `sdk default java 21.0.3-tem` |
| **EditorConfig not applying** | Reload IDE, ensure file exists at repo root |
| **Docker not starting** | Ensure Docker Desktop running (Windows) or daemon active (Linux) |

---

## 📖 Project Quick Reference

### azure-java-platform (Maven multi-module)
- **Location**: `azure-java-platform/azure-java-platform/`
- **Modules**: shared, webapi, functions
- **Build**: `mvn clean install`
- **Run Web API**: `mvn -pl webapi spring-boot:run`
- **Run Functions**: `mvn -pl functions azure-functions:run`
- **Docs**: See `docs/` folder

### farm-workers-api (Maven single)
- **Location**: `farm-workers-api/farm-workers-api/`
- **Build**: `mvn clean package`
- **Run**: `mvn spring-boot:run -Dspring-boot.run.profiles=dev`
- **API Docs**: `http://localhost:8080/swagger-ui.html`
- **Docs**: See `README.md`

### spring-academy-intro (Gradle single)
- **Location**: `spring-academy-intro/`
- **Build**: `./gradlew build`
- **Run**: `./gradlew bootRun`
- **Test**: `./gradlew test`
- **Docs**: See `HELP.md`

---

## 🎯 Next Steps

1. ✅ Install tools from appropriate section above
2. ✅ Clone repository
3. ✅ Build all projects
4. ✅ Run locally and verify at `http://localhost:8080`
5. 📖 Read individual project READMEs for more details

---

**All projects use Java 21 LTS + Spring Boot 3.3.4 + Maven 3.9.6 + Gradle 8.8.1**

Last Updated: July 31, 2026

