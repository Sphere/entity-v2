# Triage Report — aatrika-entity
**Branch:** main
**Spring Boot:** 3.4.2 | **Java:** 21 | **DB:** PostgreSQL | **Search:** OpenSearch
**Last reviewed:** 2026-06-16

---

## Summary

| Severity | Open | Completed |
|---|---|---|
| Critical | 0 | 2 |
| High | 2 | 1 |
| Medium | 3 | 1 |
| Low | 5 | 0 |

Items tagged `[security]` are security-relevant and referenced from `SECURITY.md` policy rules.

---

## Critical

### T-001 — Tomcat CVEs Unpatched ✅ COMPLETED
**Severity:** Critical
**Area:** Security / Infrastructure
**Resolved:** 2026-06-16

Spring Boot 2.7.18 defaulted to Tomcat 9.0.83 which was vulnerable to CVE-2025-24813, CVE-2025-31651, CVE-2025-55754.

**Resolution:** Spring Boot upgraded to 3.4.2 with `<tomcat.version>10.1.36</tomcat.version>` override in `pom.xml` — all three CVEs patched.

---

### T-002 — Elasticsearch Client / Server Version Mismatch Risk ✅ COMPLETED
**Severity:** Critical
**Area:** Search / Runtime
**Resolved:** 2026-06-16

Spring Boot 2.7.18 bundled ES client `~7.17.x` which was incompatible with ES 8.x server.

**Resolution:** Migrated to OpenSearch — replaced `spring-boot-starter-data-elasticsearch` with `spring-data-opensearch-starter 1.6.0`. ES client dependency fully removed.

---

## High

### T-003 — `spring.jpa.hibernate.ddl-auto=update` in Production Config `[security]`
**Severity:** High
**Area:** Database Safety

`application.properties` defaults to `ddl-auto=update` when `JPA_DDL_AUTO` env var is not set. In a production or shared environment this will auto-alter the schema on startup — dropping columns, altering types — without a controlled migration.

**File:** `src/main/resources/application.properties:13`

**Fix:** Change default to `validate` or `none`. Use a proper migration tool (Flyway / Liquibase) for schema changes.

```properties
# Change this:
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
# To:
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}
```

---

### T-004 — Dead File: `JsonMapConverter.java` ✅ COMPLETED
**Severity:** High
**Area:** Code Hygiene
**Resolved:** 2026-06-16

`JsonMapConverter` was the JPA `AttributeConverter` for the `additional_properties` column. File has been deleted — confirmed absent from the codebase.

---

### T-005 — `additional_properties` Column Still Exists in Database `[security]`
**Severity:** High
**Area:** Database

The `additional_properties` field was removed from the JPA model. With `ddl-auto=update` Hibernate will not drop the column automatically — it only adds/alters, never drops. The column remains in `master_entities` table as dead weight and will cause confusion.

**Fix:** Run a manual migration script:
```sql
ALTER TABLE master_entities DROP COLUMN IF EXISTS additional_properties;
```

---

## Medium

### T-006 — Commented-Out Code in Sheet Readers
**Severity:** Medium
**Area:** Code Hygiene

Dead commented-out blocks remain in both sheet readers:

| File | Lines | Content |
|---|---|---|
| `CsvEntitySheetReader.java` | 335–339 | `additionalProperties`, `updatedDate`, `createdDate` builder calls |
| `XlsxEntitySheetReader.java` | 21, 83–84 | `@Override` annotation, `additionalProperties` builder call |

These reference the removed `additionalProperties` field and will cause confusion about intent.

**Fix:** Remove all commented-out blocks from both reader files.

---

### T-007 — Commented-Out Query in `MasterEntityRepository`
**Severity:** Medium
**Area:** Code Hygiene

Lines 35–36 contain a commented-out `@Query` method that was replaced by the native query approach. Dead code left in repository interface.

**File:** `src/main/java/com/aastrika/entity/repository/jpa/MasterEntityRepository.java:35`

**Fix:** Delete the commented-out lines.

---

### T-008 — ES Debug Logging Enabled in Default Config `[security]` ✅ COMPLETED
**Severity:** Medium
**Area:** Observability / Performance
**Resolved:** 2026-06-16

OpenSearch log level is now guarded behind an env variable with a safe default:
```properties
logging.level.org.springframework.data.elasticsearch=${OPENSEARCH_LOG_LEVEL:WARN}
```
Note: `EntityMappingServiceImpl` still has hardcoded `DEBUG` logging — tracked separately as S-002 in `SECURITY.md`.

---

### T-009 — `getAllHeaders()` Method Unused in `EntitySheetProperties`
**Severity:** Medium
**Area:** Code Hygiene

`EntitySheetProperties.Headers.getAllHeaders()` merges required + optional + competencyLevel headers into a single list. No caller exists in the codebase — the IDE reports this as an unused method.

**File:** `src/main/java/com/aastrika/entity/config/EntitySheetProperties.java:26`

**Fix:** Either wire it into the sheet reader validation logic (replacing duplicated header-building code) or remove it.

---

## Low

### T-010 — `EntityMappingController` Injects Implementation Directly
**Severity:** Low
**Area:** Design

`EntityMappingController` injects `EntityMappingServiceImpl` instead of the `EntityMappingService` interface, creating a tight coupling between the controller and the concrete class.

**File:** `src/main/java/com/aastrika/entity/controller/EntityMappingController.java:25`

**Fix:**
```java
// Change:
private final EntityMappingServiceImpl entityMappingService;
// To:
private final EntityMappingService entityMappingService;
```

---

### T-011 — `language` Request Param Accepted but Never Used in Upload
**Severity:** Low
**Area:** API Contract

`POST /v1/entity/upload` accepts a `language` request parameter but never passes it to the service or sheet reader. The language is instead read from the sheet content itself.

**File:** `src/main/java/com/aastrika/entity/controller/EntityController.java:55`

**Fix:** Either remove the param from the API or wire it through to the service if language-level upload isolation is intended.

---

### T-012 — `CompetencyLevelResponseDTO.id` Commented Out
**Severity:** Low
**Area:** Code Hygiene

The `id` field is commented out in `CompetencyLevelResponseDTO`. If intentional (id not exposed in API), it should be deleted. If accidental, it should be restored.

**File:** `src/main/java/com/aastrika/entity/dto/response/CompetencyLevelResponseDTO.java:14`

**Fix:** Delete the commented-out line if `id` is intentionally excluded from the response.

---

### T-013 — OpenSearch Allows Empty Credentials `[security]`
**Severity:** Low
**Area:** Security / Config

`application.properties` defaults `OPENSEARCH_USERNAME` and `OPENSEARCH_PASSWORD` to empty strings. If the OpenSearch cluster permits unauthenticated access, the service connects without credentials and no error surfaces — the misconfiguration is completely silent.

**File:** `src/main/resources/application.properties:27-28`

**Fix:** Enforce non-empty credentials via deployment runbook. Optionally add a startup check:
```java
@PostConstruct
void validateOpenSearchCredentials() {
    if (username == null || username.isBlank()) {
        throw new IllegalStateException("OPENSEARCH_USERNAME must be set");
    }
}
```

---

### T-014 — `DataIntegrityViolationException` Exposes PostgreSQL Error Message `[security]`
**Severity:** Low
**Area:** Security / Error Handling

`ApplicationExceptionHandler.java:54` returns `rootCause.getMessage()` directly in the API response. A `PSQLException` root cause can contain table names, column names, and constraint names — internal schema details leaked to the caller.

**File:** `src/main/java/com/aastrika/entity/config/ApplicationExceptionHandler.java:54`

**Fix:** Return a generic message instead of the raw root cause:
```java
// Instead of:
message = rootCause.getMessage();
// Use:
message = "A data conflict occurred. Please check your input and try again.";
```

---

## Action Priority

| # | Item | Owner | Priority | Status |
|---|---|---|---|---|
| T-001 | Patch Tomcat CVEs | DevOps / Backend | Immediate | ✅ Completed 2026-06-16 |
| T-002 | Confirm ES server / migrate to OpenSearch | Infra | Immediate | ✅ Completed 2026-06-16 |
| T-003 | Change `ddl-auto` default to `validate` | Backend | Before next deploy | Open |
| T-004 | Delete `JsonMapConverter.java` | Backend | This sprint | ✅ Completed 2026-06-16 |
| T-005 | Drop `additional_properties` DB column | Backend / DBA | This sprint | Open |
| T-006 | Clean up commented code in readers | Backend | This sprint | Open |
| T-007 | Clean up commented query | Backend | This sprint | Open |
| T-008 | Guard ES debug logging | Backend | This sprint | ✅ Completed 2026-06-16 |
| T-009 | Remove or wire `getAllHeaders()` | Backend | Backlog | Open |
| T-010 | Fix interface injection in controller | Backend | Backlog | Open |
| T-011 | Resolve `language` param in upload | Backend | Backlog | Open |
| T-012 | Clean `CompetencyLevelResponseDTO` | Backend | Backlog | Open |
| T-013 `[security]` | Enforce OpenSearch credentials | Backend / Infra | Backlog | Open |
| T-014 `[security]` | Sanitise DB error message in API response | Backend | Backlog | Open |
