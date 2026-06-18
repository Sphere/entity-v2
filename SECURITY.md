# Security Policy — aatrika-entity

**Last updated:** 2026-06-16
**Service:** Spring Boot 3.4.2 microservice — master data management (Competencies, Roles, Activities)
**Stack:** Java 21 · PostgreSQL · OpenSearch · Maven

---

## 1. Scope

### In Scope
- All REST API endpoints exposed by this service (`/v1/entity/**`, `/v1/entity-mapping/**`)
- File upload processing (CSV / XLSX sheet parsing)
- PostgreSQL data access layer (JPA models, repositories, schema changes)
- OpenSearch indexing and search operations
- All runtime dependencies declared in `pom.xml`
- Application configuration and secrets handling (`application.properties`, env vars)
- Logging output — what is logged and at what level

### Out of Scope
- OpenSearch cluster infrastructure (managed separately by infra team)
- PostgreSQL server configuration and OS-level hardening
- Network perimeter, load balancers, API gateway — not owned by this service
- Authentication and authorisation — not implemented in this service; assumed to be handled upstream by a gateway or auth service
- Other microservices consuming or producing data alongside this service

### Environments Covered
| Environment | Notes |
|---|---|
| Production | Primary target — all rules in this document apply strictly |
| Staging | Apply same rules as production |
| Development / Local | Relaxed — `ddl-auto=update` and debug logging acceptable locally only |

---

## 2. Security Rules

### 2.1 Dependency Vulnerabilities
- **Tool:** OWASP Dependency-Check Maven plugin — must be added to `pom.xml` (see Pending Work in `CLAUDE.md`)
- **Policy:** Build must fail on any dependency with CVSS score ≥ 7.0 (High or Critical)
- **Cadence:** Run on every PR that touches `pom.xml`; run weekly on `main` regardless
- **Suppressions:** False positives must be documented in `owasp-suppressions.xml` with a justification comment — no silent suppression
- **Tomcat:** CVE override `<tomcat.version>10.1.36</tomcat.version>` is active in `pom.xml` — do not remove or downgrade without checking `tomcat.apache.org/security-10.html` for the patched version

### 2.2 Secrets Management
- No credentials, API keys, or connection strings in source code or `application.properties` defaults
- All sensitive values must be injected via environment variables: `DATABASE_URL`, `DATABASE_USERNAME`, `DATABASE_PASSWORD`, `OPENSEARCH_USERNAME`, `OPENSEARCH_PASSWORD`
- Default values in `application.properties` (e.g. `postgres:postgres`) are for local development only — production deployments must override all of them
- Never commit `.env` files or files containing real credentials

### 2.3 Input Validation
- All request body fields validated with `jakarta.validation` annotations (`@NotBlank`, `@NotNull`, etc.) on DTO classes
- `@Valid` on every `@RequestBody` parameter in controllers — violations caught by `MethodArgumentNotValidException` handler
- `@Validated` at service level for `@RequestParam` validation — violations caught by `HandlerMethodValidationException` handler
- File uploads: validate content type and sheet headers before processing; reject files that fail header validation via `HeaderMissingException`
- Do not trust sheet cell values — always parse defensively; missing or malformed cells must produce a `SheetDataMissingException`, not a NullPointerException

### 2.4 Database Safety
- `spring.jpa.hibernate.ddl-auto` defaults to `update` when `JPA_DDL_AUTO` is not set — **this must be overridden to `validate` or `none` in all non-local environments**
- Schema changes must go through explicit SQL migration scripts — never rely on Hibernate auto-DDL to alter production schema
- Do not use `AttributeConverter<Map, String>` for PostgreSQL `json` columns — causes silent type mismatch (`character varying` vs `json`); use `@JdbcTypeCode(SqlTypes.JSON)` instead
- Unique constraint `(code, language_code)` is enforced at DB level via `@UniqueConstraint` — do not remove this constraint
- Raw SQL queries (native queries in repositories) must use parameterised binding — no string concatenation into query strings

### 2.5 Logging Policy
- Never log PII (personally identifiable information) — user names, email addresses, IDs that map to individuals
- Never log credentials, tokens, or connection strings at any level
- OpenSearch log level defaults to `WARN` in production via `${OPENSEARCH_LOG_LEVEL:WARN}` — do not hardcode `DEBUG` in `application.properties`
- `EntityMappingServiceImpl` has `DEBUG` logging enabled permanently (`logging.level.com.aastrika.entity.service.impl.EntityMappingServiceImpl=DEBUG`) — review before production deployment; consider guarding behind an env var
- SQL logging (`spring.jpa.show-sql`) defaults to `false` — must remain `false` in production

### 2.6 Error Handling
- All exceptions must be caught by `ApplicationExceptionHandler` — never let raw stack traces reach the API response
- Error responses use `AppResponse.error(apiId, message, httpStatus)` — message must not include internal file paths, SQL, or stack traces
- `DataIntegrityViolationException` is caught and returns a sanitised conflict message — the root cause message from PostgreSQL may leak internal column names; review before exposing

### 2.7 OpenSearch
- OpenSearch credentials (`OPENSEARCH_USERNAME`, `OPENSEARCH_PASSWORD`) must be set in production even if the cluster allows unauthenticated access today — treat unauthenticated access as a misconfiguration to fix
- Do not log OpenSearch queries at `DEBUG` level in production — queries may contain user-supplied search terms

---

## 3. Security Triage

All issues — security and non-security — are tracked in `docs/TRIAGE.md` as the single source of truth.
Security-relevant items are tagged `[security]` there. Status, fixes, and completion are updated in `docs/TRIAGE.md` only.

**Current security items in TRIAGE.md:**

| TRIAGE ID | Severity | Summary |
|---|---|---|
| T-003 | High | `ddl-auto` defaults to `update` — auto-alters schema on startup |
| T-005 | High | `additional_properties` DB column not dropped — dead data in `master_entities` |
| T-008 | Medium | ES debug logging — now guarded, completed 2026-06-16 |
| T-013 | Low | OpenSearch allows empty credentials — silent unauthenticated access |
| T-014 | Low | `DataIntegrityViolationException` exposes raw PostgreSQL error message in API response |

---

## 4. Security Release Notes

Append a new entry here each time a security item is resolved or introduced.

### 2026-06-16
- **Fixed:** Tomcat CVEs patched — `<tomcat.version>10.1.36</tomcat.version>` active in `pom.xml` (CVE-2025-24813, CVE-2025-31651, CVE-2025-55754)
- **Fixed:** OpenSearch migration completed — replaced Elasticsearch 7.x client (which had known ES 8.x incompatibility risk) with `spring-data-opensearch-starter 1.6.0`
- **Fixed:** Spring Boot restored to 3.4.2 — recovers Tomcat 10.x security baseline and all Spring Security 6.x patches
- **Identified:** S-001 through S-005 documented above — none are newly introduced; all pre-existed
- **Added:** This `SECURITY.md` as the authoritative security policy document for this service
