# 🤝 Contributing to spring-boot-repo

Thank you for your interest in contributing! This guide will help you understand our development process and how to contribute effectively.

---

## 📋 Table of Contents

- [Getting Started](#getting-started)
- [Development Workflow](#development-workflow)
- [Coding Standards](#coding-standards)
- [Commit Guidelines](#commit-guidelines)
- [Pull Request Process](#pull-request-process)
- [Testing Requirements](#testing-requirements)
- [Documentation](#documentation)
- [Reporting Issues](#reporting-issues)

---

## 🚀 Getting Started

### Prerequisites

Ensure you have the following installed:

```bash
# Java 21 LTS
java -version  # Should show openjdk 21.0.3

# Maven 3.9.6
mvn --version  # Should show Apache Maven 3.9.6

# Git
git --version

# Docker (optional, for local Azure services)
docker --version
```

### Setup Development Environment

1. **Fork the Repository**
   ```bash
   # Click "Fork" on GitHub
   # Clone your fork
   git clone https://github.com/YOUR_USERNAME/spring-boot-repo.git
   cd spring-boot-repo
   ```

2. **Add Upstream Remote**
   ```bash
   git remote add upstream https://github.com/ORIGINAL_OWNER/spring-boot-repo.git
   git fetch upstream
   ```

3. **Install Development Tools**
   - **Windows**: See `SETUP_QUICK.md`
   - **Linux**: See `SETUP_QUICK.md`
   - **SDKMAN** (Linux/Mac/WSL): `sdk env install`

4. **Setup Pre-commit Hooks**
   ```bash
   # Install pre-commit framework
   pip install pre-commit

   # Install git hooks from .pre-commit-config.yaml
   pre-commit install

   # (Optional) Run against all files
   pre-commit run --all-files
   ```

5. **Configure IDE**
   - **IntelliJ IDEA**: File → Project Structure → Set SDK to Java 21
   - **VS Code**: Ctrl+Shift+P → "Java: Configure Runtime" → Select Java 21

---

## 🔄 Development Workflow

### 1. Create Feature Branch

```bash
# Sync with upstream
git fetch upstream
git checkout upstream/main

# Create feature branch from latest main
git checkout -b feature/your-feature-name

# Or for bug fixes:
git checkout -b fix/bug-description
```

**Branch Naming Convention:**
- Features: `feature/descriptive-name`
- Bug fixes: `fix/bug-description`
- Docs: `docs/update-description`
- Dependencies: `deps/update-description`
- Refactor: `refactor/description`

### 2. Make Changes

- One logical change per branch
- Keep commits atomic and focused
- Follow coding standards (see below)
- Add tests for new functionality

### 3. Commit Changes

Follow conventional commit format:

```bash
git commit -m "type(scope): subject

body

footer"
```

**Types:**
- `feat`: New feature
- `fix`: Bug fix
- `docs`: Documentation
- `style`: Code style (formatting, semicolons, etc.)
- `refactor`: Code refactoring without feature change
- `perf`: Performance improvement
- `test`: Adding or updating tests
- `chore`: Build, dependencies, tooling
- `ci`: CI/CD configuration
- `security`: Security fix

**Example:**
```bash
git commit -m "feat(webapi): add user authentication endpoint

- Implements JWT-based authentication
- Adds SecurityConfig for Spring Security
- Includes unit tests for auth service

Closes #123"
```

### 4. Run Tests Locally

```bash
# Azure Java Platform
cd azure-java-platform/azure-java-platform
mvn clean test

# Farm Workers API
cd farm-workers-api/farm-workers-api
mvn clean test

# Spring Academy Intro
cd spring-academy-intro
./gradlew test
```

### 5. Push Changes

```bash
# Push to your fork
git push origin feature/your-feature-name

# Or if you have multiple remotes:
git push origin feature/your-feature-name -u
```

### 6. Create Pull Request

- Go to GitHub and create a Pull Request
- Fill in the PR template completely
- Link related issues: `Closes #123`
- Request reviewers if needed
- Wait for CI/CD checks to pass

---

## 📝 Coding Standards

### Java Code Style

**Naming Conventions:**
- Classes: `PascalCase` (e.g., `UserService`)
- Methods: `camelCase` (e.g., `getUserById()`)
- Constants: `UPPER_SNAKE_CASE` (e.g., `MAX_RETRY_COUNT`)
- Variables: `camelCase` (e.g., `userId`)

**Formatting:**
- Use EditorConfig (auto-applied: see `.editorconfig`)
- 4-space indentation for Java
- 120 character line limit
- Unix line endings (LF)
- UTF-8 encoding

**Best Practices:**
- Use meaningful variable names
- Keep methods small and focused
- Add JavaDoc for public APIs
- Handle exceptions explicitly
- Use dependency injection (Spring)
- Avoid null pointers (use Optional)
- Write immutable objects when possible

**Example:**
```java
/**
 * Retrieves a user by their ID.
 *
 * @param userId the user's unique identifier
 * @return an Optional containing the user if found
 * @throws IllegalArgumentException if userId is null or invalid
 */
public Optional<User> getUserById(String userId) {
    if (userId == null || userId.isEmpty()) {
        throw new IllegalArgumentException("userId cannot be null or empty");
    }
    return userRepository.findById(userId);
}
```

### Test Code Style

- Follow same Java conventions
- Use descriptive test names: `testGetUserByIdWhenUserExists()`
- Arrange-Act-Assert (AAA) pattern
- Mock external dependencies
- Keep tests focused and isolated

**Example:**
```java
@Test
void testGetUserByIdWhenUserExists() {
    // Arrange
    String userId = "user-123";
    User expectedUser = new User(userId, "John Doe");
    when(userRepository.findById(userId)).thenReturn(Optional.of(expectedUser));

    // Act
    Optional<User> result = userService.getUserById(userId);

    // Assert
    assertTrue(result.isPresent());
    assertEquals(expectedUser, result.get());
    verify(userRepository).findById(userId);
}
```

### Markdown Style

- Use clear headings
- Include code examples
- Keep lines readable (80 chars recommended)
- Use consistent formatting
- Link related docs

---

## 📨 Commit Guidelines

### Message Format

```
<type>(<scope>): <subject>
<blank line>
<body>
<blank line>
<footer>
```

### Subject Line (50 characters max)
- Use imperative mood ("add" not "added" or "adds")
- Don't capitalize first letter
- No period at the end
- Reference issues if applicable

### Body (Optional)
- Explain what and why, not how
- Wrap at 72 characters
- Separate from subject with blank line

### Footer (Optional)
- Reference issues: `Closes #123`, `Fixes #456`
- Note breaking changes: `BREAKING CHANGE: description`

### Examples

**Good:**
```
feat(auth): add jwt token validation

Implement JWT validation for API endpoints to enhance security.
Validates token expiration and signature.

Closes #42
```

**Bad:**
```
Updated files
```

---

## 🔀 Pull Request Process

### Before Creating PR

- [ ] Branch created from latest `upstream/main`
- [ ] All tests pass: `mvn test` or `./gradlew test`
- [ ] Code follows style guidelines
- [ ] No debug code or commented-out lines
- [ ] Documentation updated
- [ ] Commit messages follow conventional commits
- [ ] Pre-commit hooks pass: `pre-commit run --all-files`

### PR Template

Use the provided PR template (auto-filled on GitHub). Include:

- **Description**: What does this PR do?
- **Type**: Bug fix / Feature / Documentation / Refactor
- **Related Issues**: Closes #123, Fixes #456
- **Testing**: How was this tested?
- **Checklist**: Mark items as complete

### Code Review

- Respond to feedback constructively
- Make requested changes in new commits
- Re-request review after changes
- Squash commits before merge if requested
- Approve = ready to merge

### Merge Criteria

- ✅ All CI/CD checks pass
- ✅ At least one approval from maintainer
- ✅ All conversations resolved
- ✅ Branch is up-to-date with main

---

## ✅ Testing Requirements

### Unit Tests

```bash
# Run all unit tests
mvn test

# Run specific test class
mvn test -Dtest=UserServiceTest

# Run specific test method
mvn test -Dtest=UserServiceTest#testGetUserById
```

### Integration Tests

```bash
# Run integration tests (typically marked with @SpringBootTest)
mvn test -Dgroups=integration
```

### Test Coverage

Aim for > 80% code coverage on new code:

```bash
# Generate coverage report
mvn test jacoco:report

# View report at: target/site/jacoco/index.html
```

### Test Guidelines

- One assertion per test when possible
- Use meaningful test names
- Mock external dependencies
- Test both happy path and error cases
- Include integration tests for APIs

---

## 📚 Documentation

### Code Documentation

- Add JavaDoc to all public classes and methods
- Include `@param`, `@return`, `@throws` tags
- Provide usage examples in JavaDoc when helpful

### Project Documentation

- Update `README.md` for new features
- Keep `DEVELOPMENT.md` current
- Document architectural decisions in ADRs
- Update setup guides if prerequisites change

### Commit Documentation

- Write clear commit messages
- Explain the "why" not just the "what"
- Reference related issues and PRs

---

## 🐛 Reporting Issues

### Before Opening an Issue

- Check existing issues (open and closed)
- Search with keywords
- Check documentation and FAQs
- Verify with latest `main` branch

### Issue Template

Provide:
- **Description**: Clear summary of the issue
- **Steps to Reproduce**: Specific steps to recreate
- **Expected Behavior**: What should happen
- **Actual Behavior**: What actually happened
- **Environment**: Java version, OS, IDE, etc.
- **Logs/Screenshots**: Any relevant error messages
- **Workaround**: Temporary solution if available

### Security Issues

⚠️ **Do NOT open a public issue for security vulnerabilities**

Instead:
1. Email security@example.com with details
2. Include: description, steps to reproduce, impact
3. Allow reasonable time for fix before disclosure
4. We'll credit you appropriately

---

## 🛠️ Development Tools & Scripts

### Useful Commands

```bash
# Build all projects
mvn clean install -DskipTests

# Build with tests
mvn clean install

# Run a specific project
cd azure-java-platform/azure-java-platform
mvn -pl webapi spring-boot:run

# Format code
mvn spotless:apply

# Generate dependency report
mvn dependency:report

# Check for vulnerabilities
mvn org.owasp:dependency-check-maven:check
```

### IDE Configuration

**IntelliJ IDEA:**
- Import formatter: File → Settings → Editor → Code Style → Import Scheme → EditorConfig
- Enable EditorConfig: File → Settings → Editor → Code Style → Enable EditorConfig

**VS Code:**
- Install extension: "EditorConfig for VS Code"
- Automatically applies `.editorconfig` rules

---

## 📋 Developer Checklist

Before submitting a PR, ensure:

- [ ] Feature branch from `upstream/main`
- [ ] Code compiles: `mvn clean compile`
- [ ] Tests pass: `mvn test`
- [ ] Coverage > 80% on new code
- [ ] Code style followed (EditorConfig)
- [ ] JavaDoc added to public APIs
- [ ] No debug code or TODOs
- [ ] Commit messages conventional
- [ ] Pre-commit hooks pass
- [ ] PR template filled out
- [ ] Related issues linked
- [ ] Documentation updated

---

## 🎓 Learning Resources

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Maven Guide](https://maven.apache.org/guides/)
- [Gradle User Manual](https://docs.gradle.org/current/userguide/)
- [Conventional Commits](https://www.conventionalcommits.org/)
- [Java Coding Standards](https://google.github.io/styleguide/javaguide.html)

---

## 🤝 Community

- **GitHub Discussions**: Ask questions, discuss ideas
- **Issues**: Report bugs, request features
- **Pull Requests**: Contribute code improvements
- **Documentation**: Help improve docs

---

## ❓ Questions?

1. Check existing issues and discussions
2. Read `DEVELOPMENT.md` for more details
3. Review relevant project README
4. Open a GitHub discussion

---

**Happy contributing!** 🚀

---

**Last Updated**: July 31, 2026

