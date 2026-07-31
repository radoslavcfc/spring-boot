# Architectural Decision Records (ADRs)

This directory contains Architectural Decision Records (ADRs) for the spring-boot-repo project.

## What is an ADR?

An ADR is a document that captures an important architectural decision made along with its context and consequences.

## Format

Each ADR follows this template:

```
# ADR-XXX: [Title]

## Status
[Proposed | Accepted | Deprecated | Superseded]

## Context
[Describe the issue we're facing and why it matters]

## Decision
[Describe what we decided to do and why]

## Consequences
[Describe the resulting context, both positive and negative]

## Alternatives Considered
[List other options we considered]

## References
[Links to related documents, issues, or discussions]
```

## Active ADRs

### ADR-001: Use Java 21 LTS

- **Status**: Accepted
- **Date**: 2026-07-31
- **Issue**: Version standardization across projects

**Context**: Projects had mixed Java versions (25, 17, 21).

**Decision**: Standardize on Java 21 LTS for stability, Azure compatibility, and long-term support (until Sept 2031).

**Consequences**: 
- ✅ Official Azure Functions support
- ✅ 7 years of LTS support
- ✅ Better tooling ecosystem
- ⚠️ Requires Java 21 on all developer machines

**Alternatives Considered**:
- Java 25 (experimental, short lifecycle)
- Java 17 (older LTS, limited features)

---

### ADR-002: Use Spring Boot 3.3.4 LTS

- **Status**: Accepted
- **Date**: 2026-07-31
- **Issue**: Spring Boot version inconsistency

**Context**: Projects used Spring Boot 4.0.6 (experimental) and 3.3.4 (LTS).

**Decision**: Standardize on Spring Boot 3.3.4 LTS for production stability and compatibility with Java 21.

**Consequences**:
- ✅ Stable, battle-tested version
- ✅ Full Java 21 support
- ✅ Years of maintenance (until Dec 2025)
- ⚠️ Downgrade from 4.0.6 for some projects

**Alternatives Considered**:
- Spring Boot 4.0.6 (cutting-edge but experimental)
- Spring Boot 3.2.x (older LTS)

---

### ADR-003: Multi-Module Maven for azure-java-platform

- **Status**: Accepted
- **Date**: 2026-07-31
- **Issue**: Code organization for complex project

**Context**: azure-java-platform includes web API, Azure Functions, and shared code.

**Decision**: Use Maven multi-module structure to share common code and dependencies.

**Consequences**:
- ✅ Single dependency declaration
- ✅ Shared parent POM
- ✅ Easy module references
- ⚠️ More complex build system

**Alternatives Considered**:
- Single Maven module (code duplication)
- Gradle multi-project (unfamiliar to team)
- Separate repositories (versioning complexity)

---

### ADR-004: EditorConfig for Cross-IDE Consistency

- **Status**: Accepted
- **Date**: 2026-07-31
- **Issue**: Different developers use different IDEs

**Context**: Team uses IntelliJ IDEA and VS Code with different default formatting.

**Decision**: Add `.editorconfig` for consistent formatting across all IDEs.

**Consequences**:
- ✅ Same formatting on all machines
- ✅ No merge conflicts from formatting
- ✅ No IDE-specific configuration needed
- ✅ Works with all major IDEs

**Alternatives Considered**:
- IDE-specific formatters (requires per-IDE setup)
- Spotless/Checkstyle (only catches issues, doesn't format)

---

### ADR-005: SDKMAN for Version Management

- **Status**: Accepted
- **Date**: 2026-07-31
- **Issue**: Java, Maven, Gradle version management

**Context**: Different developers need exact same versions.

**Decision**: Add `.sdkmanrc` for SDKMAN version management (Linux/Mac/WSL).

**Consequences**:
- ✅ One-command setup: `sdk env install`
- ✅ Automatic version switching per project
- ✅ No PATH conflicts
- ⚠️ Only works on Linux/Mac/WSL (not Windows CMD)

**Alternatives Considered**:
- Manual installation (error-prone)
- JEnv/nvm (tool-specific, less complete)
- Docker (overkill for dev setup)

---

### ADR-006: Comprehensive .gitignore

- **Status**: Accepted
- **Date**: 2026-07-31
- **Issue**: Preventing accidental commits of sensitive files

**Context**: Multiple IDEs, build tools, and Azure services.

**Decision**: Create comprehensive `.gitignore` files with sections for all tools, IDEs, secrets.

**Consequences**:
- ✅ No accidental commits of build artifacts
- ✅ No secrets committed
- ✅ Clean repository
- ✅ Works across all teams

**Alternatives Considered**:
- Global gitignore (per-user, not team-wide)
- No gitignore (requires manual vigilance)

---

## ADR Process

### Creating a New ADR

1. Create new file: `ADR-XXX.md` (increment number)
2. Use template above
3. Write decision and consequences
4. Open PR with ADR
5. Discuss with team
6. Accept or reject
7. Update this index

### Status Progression

- **Proposed**: Initial, under discussion
- **Accepted**: Approved and implemented
- **Deprecated**: No longer applies
- **Superseded**: Replaced by new ADR

---

## References

- [ADR Decision Template](https://github.com/joelparkerhenderson/architecture_decision_record)
- [Documenting Architecture Decisions](https://cognitiveclass.ai/blog/documenting-architecture-decisions/)
- [ADRs as Code](https://github.com/npryce/adr-tools)

---

**Last Updated**: July 31, 2026

