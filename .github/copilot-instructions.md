# Diocese Dashboard - Copilot Instructions

Spring Boot 3.2.5 / Java 22 web application for church service reporting for the Episcopal Church in Costa Rica. Replaces a Google Forms workflow.

## Build & Run Commands

```bash
# Build and package
mvn clean package

# Run the application
mvn spring-boot:run

# Run tests
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Generate schema.sql from JPA entities
mvn compile exec:java

# Run checkstyle
mvn checkstyle:check

# Apply OpenRewrite code cleanup
mvn rewrite:run
```

## Required Environment Variables

The app will not start without these:

| Variable | Purpose |
|---|---|
| `POSTGRESQL_PORT` | PostgreSQL port (e.g. `5432`) |
| `SPRING_DATABASE_NAME` | Database name |
| `SPRING_DATASOURCE_USERNAME` | DB username |
| `SPRING_DATASOURCE_PASSWORD` | DB password |

**Note:** `spring.jpa.hibernate.ddl-auto=create` — the schema is **dropped and recreated on every startup**.

## Architecture

### Layer Structure

```
org.iecr.diocesedashboard/
├── domain/
│   ├── objects/       # JPA entities
│   └── repositories/  # Spring Data JPA interfaces (extend JpaRepository)
├── service/           # Thin service classes wrapping repositories
├── controller/        # (empty — no web endpoints yet)
└── webapp/            # Bootstrap: main class, startup listener, SchemaGenerator
```

### Domain Model

- **Church** (PK: `name` String) — central entity; has one main `Celebrant`, many `ServiceInstance`s
- **Celebrant** (PK: Long) — clergy member; ManyToMany with `ServiceInstance`
- **ServiceTemplate** (PK: Long) — blueprint for a service type; has many `ServiceInfoItem`s
- **ServiceInstance** (PK: Long) — a concrete service at a `Church` using a `ServiceTemplate`; links celebrants
- **ServiceInfoItem** (PK: Long) — a survey question on a template; type is `ServiceInfoItemType` enum (NUMERICAL, DOLLARS, COLONES, STRING)
- **ServiceInfoItemResponse** — a response to a `ServiceInfoItem` on a `ServiceInstance`

### Schema Generation

`SchemaGenerator` (in `webapp/`) uses Hibernate + Reflections to scan `domain.objects`, build a `MetadataSources` config, and export DDL to `schema.sql`. Run via `mvn compile exec:java`. This file is committed and used as the reference schema.

### Service Pattern

Services are thin pass-throughs to repositories — no business logic currently lives there. When adding features, keep domain logic in the service layer, not controllers or repositories.

## Key Conventions

### Code Style (Google Java Style via Checkstyle)

- **2-space indentation** (enforced by `checkstyle.xml`)
- **100-character line limit** for Java files
- No star imports (`AvoidStarImport`)
- Imports: static first, then third-party, sorted alphabetically within groups
- Public methods with >2 lines require Javadoc
- Protected/public types require Javadoc (`MissingJavadocType`)
- Method names must match `^[a-z][a-z0-9]\w*$` — first two chars must be lowercase letters/digits
- Member names must be at least 2 characters: `^[a-z][a-z0-9][a-zA-Z0-9]*$`
- Use `// CHECKSTYLE.OFF: RuleName` / `// CHECKSTYLE.ON: RuleName` to suppress locally

### JPA / Database

- Entity table names use **PascalCase** (e.g., `Church`, `ServiceInstance`) — not snake_case
- Join table for the ManyToMany is `celebrant_service`
- Sequences use increment-by-50 (Hibernate allocationSize default)
- `Church.name` is the primary key (String) — not a generated ID

### Dependency Injection

- Constructor injection with `@Autowired` is used throughout
- Lombok is available (`@Data`, `@Builder`, etc.) — use it for boilerplate reduction in entities/DTOs
