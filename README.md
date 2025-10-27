
# GeoBank Santander — Geospatial Banking Platform

## Executive Overview

GeoBank Santander is an enterprise-grade, geospatially-enabled banking platform designed for robust branch management, regulatory compliance, and high-availability operations. Built with Clean/Hexagonal Architecture, CQRS, and DDD, it delivers maintainability, scalability, and security for mission-critical financial services.

---

## Table of Contents

1. [System Vision](#system-vision)
2. [Architecture](#architecture)
3. [Getting Started](#getting-started)
4. [Configuration](#configuration)
5. [API Reference](#api-reference)
6. [Development Workflow](#development-workflow)
7. [Testing & Quality](#testing--quality)
8. [Security & Compliance](#security--compliance)
9. [Troubleshooting & FAQ](#troubleshooting--faq)
10. [Contributing](#contributing)
11. [License](#license)

---

## 1. System Vision

GeoBank enables:

- Geospatial branch management with high-precision coordinate validation
- Secure, token-based authentication and auditability
- High-performance caching and observability
- Regulatory compliance (PCI DSS, LGPD, PSD2)
- Extensible, testable, and maintainable codebase

---

## 2. Architecture

**Key Principles:**

- Clean/Hexagonal Architecture: strict separation of concerns
- CQRS: command/query responsibility segregation
- DDD: explicit bounded contexts and business logic
- KISS & SOLID: simplicity and maintainability

**Layered Structure:**

```
├── api/            # REST controllers, DTOs, validation
├── application/    # Use cases, orchestration, workflows
├── domain/         # Entities, value objects, business rules
├── infrastructure/ # Persistence, config, external integrations
└── shared/         # Utilities, cross-cutting concerns
```

**See [`docs/ARCHITECTURE.md`](docs/ARCHITECTURE.md) for full details and diagrams.**

---

## 3. Getting Started

### Prerequisites

- Java 17+ (LTS recommended)
- Maven 3.6+
- Docker (optional, for containerized execution)

### Local Setup

```bash
git clone https://github.com/cezicolaseniorsoftwareengineer/geobanksantander.git
cd geobank-api
mvn clean spring-boot:run
```

App available at: [http://localhost:8080/api/v1](http://localhost:8080/api/v1)

### Docker Setup

```bash
docker build -t geobank-api .
docker run -p 8080:8080 geobank-api
```

### Environment Variables

See `src/main/resources/application.yml` for all configuration options. Override via `-D` flags or environment variables as needed.

---

## 4. Configuration

**application.yml (default):**

```yaml
spring:
  cache:
    type: caffeine
    caffeine:
      spec: expireAfterWrite=5m,maximumSize=1000
  datasource:
    url: jdbc:h2:mem:geobank;DB_CLOSE_DELAY=-1
    username: sa
    password:
  profiles:
    active: default
```

**Production Profile:**

```bash
mvn spring-boot:run -Dspring-boot.run.profiles=production
```

---

## 5. API Reference

### Authentication

```http
POST /api/v1/auth/login
{
  "username": "admin",
  "password": "geobank123"
}
# Returns: Bearer token
```

### Register Branch

```http
POST /api/v1/desafio/cadastrar?posX=-23.5505&posY=-46.6333
Authorization: Bearer <token>
# Returns: Branch registration confirmation
```

### Distance Query

```http
GET /api/v1/desafio/distancia?posX=-23.5505&posY=-46.6333
Authorization: Bearer <token>
# Returns: List of branches ordered by proximity
```

### Health & Metrics

```http
GET /actuator/health
GET /actuator/info
GET /api/v1/cache-test/metrics
```

**See [`docs/API.md`](docs/API.md) for full endpoint documentation and examples.**

---

## 6. Development Workflow

### Branching & Commits

- Use `main` for production-ready code
- Feature branches: `feature/<name>`
- Commit messages: imperative, concise, in English (e.g., `Add branch registration endpoint`)

### Code Reviews & PRs

- All code changes require PR and review
- Enforce code style, tests, and documentation

### CI/CD

- Recommended: GitHub Actions or Jenkins for build, test, and deploy
- Quality gates: unit/integration tests, mutation testing, SonarQube analysis

---

## 7. Testing & Quality

### Testing Strategy

- Unit: JUnit 5, Mockito (domain, value objects, business rules)
- Integration: Testcontainers, Spring Boot Test (API, DB, cache)
- Contract: OpenAPI/Pact (API schemas)
- Mutation: PIT (mutation coverage >85%)
- Code Quality: SonarQube, Checkstyle, PMD, SpotBugs

### Coverage Targets

- Unit test coverage: >90%
- Integration test coverage: >85%
- Mutation score: >85%
- Technical debt ratio: <5%

**See [`CODE_QUALITY_TESTING_STRATEGY.md`](CODE_QUALITY_TESTING_STRATEGY.md) for details.**

---

## 8. Security & Compliance

- Input validation and sanitization at all layers
- Token-based authentication (JWT)
- HTTPS-ready, secure headers
- Audit trail: correlation IDs, structured logs
- Compliance: PCI DSS, LGPD, PSD2
- No sensitive data in error responses

---

## 9. Troubleshooting & FAQ

### Common Issues

- **403 Forbidden:** Check Authorization header and credentials
- **400 Bad Request:** Invalid coordinates or missing parameters
- **Performance:** Monitor cache hit ratio (>70%), check Actuator metrics

### Useful Commands

```bash
mvn test                # Run all tests
mvn verify              # Run tests + quality checks
docker-compose up       # (if docker-compose.yml present)
```

---

## 10. Contributing

We welcome contributions from experienced engineers. Please:

- Follow the architecture and code style guidelines
- Write clear, English commit messages
- Add/maintain tests for all code
- Document all endpoints and features
- Open a PR for review

See [`CONTRIBUTING.md`](CONTRIBUTING.md) for full guidelines.

---

## 11. License

Proprietary system — Santander Brasil S.A. All rights reserved.

---

**Contact:**
Senior Software Engineer — [redacted]
Banking Systems & RegTech Specialist

---

*"Banking systems demand precision, security, and regulatory compliance. This system delivers all three with enterprise-grade architecture and KISS principles."*
