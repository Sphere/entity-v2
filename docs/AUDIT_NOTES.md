# CLAUDE.md Audit Notes — 2026-06-16

Audit of conflicts found between CLAUDE.md rules and the actual codebase state.
Captured before CLAUDE.md was corrected. Kept as historical reference.

---

## 1. Tech Stack table — all stale

| CLAUDE.md said | Actual code | File |
|---|---|---|
| Spring Boot 2.7.18 | 3.4.2 | `pom.xml:8` |
| Java 17 | 21 | `pom.xml:30` |
| Hibernate 5.6 / Spring Data JPA 2.7 | Hibernate 6 / Spring Data JPA 3.x | (comes with Boot 3) |
| Spring Data Elasticsearch 4.x (→ OpenSearch) | OpenSearch already in use | `pom.xml:41-45` |
| MapStruct 1.5.5 | 1.6.3 | `pom.xml:31` |
| springdoc-openapi-ui 1.8.0 | springdoc-openapi-starter-webmvc-ui 2.8.4 | `pom.xml:88-91` |

---

## 2. Import rule — directly wrong, enforced on every file

**CLAUDE.md said:** `Use javax.* imports — NOT jakarta.*`

**Reality:** Every source file uses `jakarta.*` — JPA models, DTOs, controllers, readers. Zero `javax.*` in the codebase. Following this rule would produce code that does not compile.

---

## 3. HandlerMethodValidationException — banned but already in use

**CLAUDE.md said:** `Do NOT use HandlerMethodValidationException — Spring 6 only, not available in Spring 5`

**Reality:** `ApplicationExceptionHandler.java:39` has a live `@ExceptionHandler(HandlerMethodValidationException.class)` — correct since Spring Boot 3 runs on Spring 6.

---

## 4. Serializable on JPA models — opposite of reality

**CLAUDE.md said:** `All JPA entity classes must implement java.io.Serializable`

**Reality:** Neither `MasterEntity` nor `CompetencyLevel` implements `Serializable` — correct for Hibernate 6 which no longer requires it.

---

## 5. additional_properties — CLAUDE.md said removed, still in the model

**CLAUDE.md Release Notes said:** "Removed `additional_properties` field from `MasterEntity`, all DTOs..."

**Reality:** `MasterEntity.java:98-100` still has the field with `@JdbcTypeCode(SqlTypes.JSON)`. TRIAGE.md T-005 also confirms the DB column was never dropped.

---

## 6. JsonMapConverter.java — CLAUDE.md said do not delete, already gone

**CLAUDE.md said:** `Do not delete JsonMapConverter.java until confirmed no other module references it`

**Reality:** File does not exist. Already deleted. The rule was dead weight.

---

## 7. Pending Work table — items marked pending were already done

| CLAUDE.md Pending item | Actual status |
|---|---|
| OpenSearch migration | Done — `spring-data-opensearch-starter` in `pom.xml` |
| Tomcat CVE patch | Done — `<tomcat.version>10.1.36</tomcat.version>` in `pom.xml` |
| `JsonMapConverter.java` cleanup | Done — file is gone |
| `additional_properties` DB column drop | Still pending — model still has the field, T-005 open |

---

## 8. ElasticsearchConfig.java — inconsistent post-migration

`ElasticsearchConfig.java:4` still imports `@EnableElasticsearchRepositories` from `spring-data-elasticsearch` — but that dependency was replaced by OpenSearch. Needs rewrite for OpenSearch config.

---

## Root Cause

The OpenSearch migration (and related Spring Boot 3 / Java 21 upgrade) was executed in the codebase but CLAUDE.md was never updated to reflect the new state. The "Planned" section described the migration accurately — it was just never promoted to the main rules after execution.