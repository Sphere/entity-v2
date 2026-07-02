# Claude Code Rules — aatrika-entity
<!-- This file is read automatically by Claude Code. Non-Claude developers: treat this as the project coding standards. -->

## Meta Rules
- Read `docs/DECISIONS.md` before implementing any code change.
- Before any security-related work — dependency changes, vulnerability assessment, logging, error handling, DB schema changes — read `SECURITY.md` for scope, rules, and current triage status.
- Before any major change — new feature, dependency upgrade, DB schema change, API contract change — check `docs/TRIAGE.md` for open items tagged `[security]` or matching the area being changed. Flag any relevant open items to the user before implementing.
- At the end of a session where significant code was changed, ask the user: "Should I draft a CHANGELOG entry?" If yes, run `bash scripts/update-release-notes.sh <version>` — use `docs/RELEASE_NOTE_TEMPLATE.md` as the structure. Do not apply automatically; show the draft first.
- If any rule in this file conflicts with the actual code, stop and flag the conflict to the user before proceeding.
- **HARD RULE — No git operations:** Never run `git commit`, `git push`, `git tag`, `git merge`, `git rebase`, `git reset`, or any command that writes to the repository. Provide the exact command as a suggestion for the user to run themselves. This rule has no exceptions.

---

## Project Overview
Spring Boot microservice for master data management — Competencies, Roles, Activities and other entity types.
Persists to **PostgreSQL** (JPA/Hibernate), indexes to **OpenSearch**.

---

## Tech Stack
| Layer | Technology |
|---|---|
| Framework | Spring Boot 3.4.2 |
| Language | Java 21 |
| ORM | Hibernate 6 / Spring Data JPA 3.x |
| Search | spring-data-opensearch-starter 1.6.0 |
| Mapping | MapStruct 1.6.3 + Lombok |
| Validation | jakarta.validation |
| API Docs | springdoc-openapi-starter-webmvc-ui 2.8.4 |
| DB | PostgreSQL |
| Build | Maven |
| Tomcat | 10.1.36 (CVE override in pom.xml) |

---

## Package Structure
```
com.aastrika.entity
├── common/          # Constants only — no logic
├── config/          # Spring config beans, exception handler, properties binding
├── controller/      # REST controllers — thin, no business logic
├── document/        # OpenSearch document classes (@Document)
├── dto/
│   ├── request/     # Inbound DTOs
│   └── response/    # Outbound DTOs
├── enums/           # EntityType (dynamic utility class, not a Java enum) and other types
├── exception/       # Custom runtime exceptions extending ApiRuntimeException
├── mapper/          # MapStruct interfaces only
├── model/           # JPA entities
├── reader/          # Sheet parsers (CSV / XLSX)
├── repository/
│   ├── es/          # OpenSearch repositories
│   └── jpa/         # Spring Data JPA repositories
├── service/         # Service interfaces
│   └── impl/        # Service implementations
└── util/            # Stateless utility classes
```

---

## Code Generation Rules

### General
- Use `jakarta.*` imports — NOT `javax.*` (Spring Boot 3.x / Jakarta EE 10 baseline)
- JPA entity classes do NOT need to implement `java.io.Serializable` — Hibernate 6 does not require it
- Use Lombok: `@Data`, `@Builder`, `@NoArgsConstructor`, `@AllArgsConstructor` on all DTOs and models
- No business logic in controllers — delegate entirely to service layer
- Controllers inject service **interfaces**, not impl classes (exception: `EntityMappingController` currently injects impl — do not replicate this pattern)
- Never add `@Transactional` to controllers

### Naming Conventions
- Controllers: `<Domain>Controller`
- Services: `<Domain>Service` (interface) + `<Domain>ServiceImpl` (impl)
- DTOs: `<Action>RequestDTO` / `<Action>ResponseDTO`
- JPA models: plain class name — `MasterEntity`, `CompetencyLevel`
- OpenSearch documents: `<Name>Document`
- MapStruct mappers: `<Domain>Mapper`

### Validation
- Use `@NotBlank`, `@NotNull` from `jakarta.validation.constraints` on DTO fields
- `@Valid` on `@RequestBody` → caught by `MethodArgumentNotValidException` handler
- `HandlerMethodValidationException` handles `@RequestParam` / service-level `@Validated` violations — this is valid in Spring 6 (Spring Boot 3.x)

### Exception Handling
- All custom exceptions extend `ApiRuntimeException`
- Throw domain-specific exceptions (`UploadEntityException`, `UpdateEntityException`) — never throw raw `RuntimeException`
- `ApplicationExceptionHandler` is the single `@RestControllerAdvice` — add new handlers there only

### Responses
- All endpoints return `AppResponse` wrapper
- Use `AppResponse.success(apiId, result, httpStatus)` for success
- Use `AppResponse.error(apiId, message, httpStatus)` for errors
- API ID format: `api.entity.<action>` (e.g. `api.entity.create`, `api.entity.mapping`)

### MapStruct
- Define mappings as interfaces only — no manual mapping logic in mapper files
- Use `@Mapping(target = "field", ignore = true)` explicitly for all fields not mapped by name
- Custom conversions go in `default` methods inside the mapper interface

### JPA / Database
- Entity unique constraint is `(code, language_code)` — always enforce at DB level via `@UniqueConstraint`
- `CompetencyLevel` joins `MasterEntity` via `(code, language_code)` — not by surrogate `id`
- For `json` / `jsonb` columns use `@JdbcTypeCode(SqlTypes.JSON)` with `@Column(columnDefinition = "json")` — this is the Hibernate 6 approach and works correctly with PostgreSQL
- Do not use `AttributeConverter<Map, String>` for json columns — causes `column is of type json but expression is of type character varying`

### EntityType
- `EntityType` is a `final class`, NOT a Java enum — it has no `valueOf()`, no `name()`, no `ordinal()`
- Valid types are loaded at startup from `entity.entityTypeList` (application.properties) via `EntityStartupApplicationRunner` which calls `EntityType.load()`
- Only `EntityType.COMPETENCY = "COMPETENCY"` is a compile-time constant — use it where needed
- For all other types (`ROLE`, `POSITION`, `ACTIVITY`, etc.) use plain string literals — never assume a constant exists
- Use `EntityType.validate(String)` to guard user-supplied values at service boundaries
- In tests: use string literals (`"ROLE"`, `"POSITION"`) not `EntityType.ROLE` — those constants do not exist

### OpenSearch
- All OpenSearch operations go through `MasterEntityEsService` — controllers must not call OpenSearch repositories directly
- Document ID format: `<code>_<languageCode>`
- Keep search logic in `MasterEntityEsServiceImpl` — it is the single point for OpenSearch interaction
- `saveEntityDetailsInES(List<EntitySheetRow>, String entityType, String userId)` — always pass `userId`; the impl sets `createdAt` (current timestamp) and `createdBy` (userId) on each document

### Properties Binding
- Sheet column config lives under `entity-sheet.*` prefix — bound via `EntitySheetProperties` (`@ConfigurationProperties`)
- Do not hardcode sheet header names in business logic — always reference `EntitySheetHeadersConstant`
- `entity.entityTypeList` — comma-separated list of valid entity types loaded at startup; adding a new entity type requires updating this property
- `entity-map.allowed-type-combinations` — comma-separated parent_child pairs that control which hierarchy mappings are permitted; currently: `ORGANIZATION_POSITION, POSITION_ROLE, ROLE_ACTIVITY, ACTIVITY_COMPETENCY, STATE_DISTRICT, DISTRICT_BLOCK, BLOCK_FACILITY, FACILITY_POSTING_FACILITY, BLOCK_POSTING_FACILITY`

### Do NOT
- Do not use `javax.*` imports — the codebase is on `jakarta.*`
- Do not add `implements Serializable` to JPA entities — not required by Hibernate 6
- Do not create new `AttributeConverter` that returns `String` for PostgreSQL `json` columns — use `@JdbcTypeCode(SqlTypes.JSON)` instead
- Do not inject `ElasticSearchEntityRepository` directly into controllers or service impl outside `MasterEntityEsServiceImpl`
- Do not add `@Transactional` to controllers

---

## Pending Work (as of 2026-07-02)

| Item | Status | Notes |
|---|---|---|
| `additional_properties` DB column drop | Pending | Column still exists in `master_entities` table — run `ALTER TABLE master_entities DROP COLUMN IF EXISTS additional_properties;` |
| `ElasticsearchConfig.java` rewrite | Pending | Still uses `@EnableElasticsearchRepositories` from spring-data-elasticsearch — inconsistent with OpenSearch migration; needs replacement with OpenSearch-native config |
| `additional_properties` field in `MasterEntity` | Under review | Field still present in model (`MasterEntity.java`) with `@JdbcTypeCode(SqlTypes.JSON)` — confirm with team whether to keep or remove |

---

## Release Notes

### [Current] — 2026-07-02

#### Completed
- `spring-data-opensearch-starter` upgraded `1.5.3 → 1.6.0` — fixes `NoSuchMethodError` (`SearchDocumentResponse` constructor mismatch) caused by Spring Boot 3.4.2 pulling `spring-data-elasticsearch 5.4.x`
- `EntityType` refactored from Java enum to `final class` with runtime-loaded type set; types driven by `entity.entityTypeList` property loaded at startup via `EntityStartupApplicationRunner`
- `saveEntityDetailsInES` signature updated to include `userId` (3rd param) — sets `createdBy` and `createdAt` on each OpenSearch document
- `entity-map.allowed-type-combinations` expanded to 9 combinations covering the full hierarchy: ORGANIZATION → POSITION → ROLE → ACTIVITY → COMPETENCY and STATE → DISTRICT → BLOCK → FACILITY/POSTING_FACILITY
- Test suite fixed for `EntityType` enum→class refactor: removed `valueOf()` calls, replaced enum constants with string literals, fixed `any(EntityType.class)` matchers
- Test suite fixed for OpenSearch mock alignment: `ElasticsearchOperations`/`NativeQuery` replaced with `OpenSearchOperations`/`NativeSearchQuery` in `MasterEntityEsServiceImplTest`

#### Known Issues
- `ElasticsearchConfig.java` still uses `@EnableElasticsearchRepositories` — inconsistent with OpenSearch migration, needs update
- `additional_properties` DB column not yet dropped from `master_entities`
- `additional_properties` field still present in `MasterEntity` model — pending team decision

---

### [Historical] — 2026-06-16

#### Completed
- OpenSearch migration done: replaced `spring-boot-starter-data-elasticsearch` with `spring-data-opensearch-starter 1.6.0`
- Spring Boot restored to 3.4.2, Java restored to 21, MapStruct restored to 1.6.3
- All imports migrated from `javax.*` to `jakarta.*`
- `springdoc-openapi-ui` v1 replaced with `springdoc-openapi-starter-webmvc-ui` v2
- Tomcat CVE patch active: `<tomcat.version>10.1.36</tomcat.version>` in `pom.xml` — covers CVE-2025-24813, CVE-2025-31651, CVE-2025-55754
- `HandlerMethodValidationException` handler restored in `ApplicationExceptionHandler` — valid in Spring 6
- `JsonMapConverter.java` deleted — was unused after `additional_properties` mapping was removed from the converter layer
- `Serializable` removed from JPA models — not required by Hibernate 6

#### Known Issues
- `ElasticsearchConfig.java` still uses `@EnableElasticsearchRepositories` — inconsistent with OpenSearch migration, needs update
- `additional_properties` DB column not yet dropped from `master_entities` (T-005)
- `additional_properties` field still present in `MasterEntity` model — pending team decision on whether to retain

---

### [Historical] — 2026-06-15

#### Changed (downgrade, now reversed)
- Downgraded Spring Boot `3.4.2 → 2.7.18` and Java `21 → 17` to align with Elasticsearch 7.x server constraint
- All `jakarta.*` imports replaced with `javax.*`
- MapStruct downgraded `1.6.3 → 1.5.5.Final`
- `springdoc-openapi-starter-webmvc-ui` v2 replaced with `springdoc-openapi-ui` v1
- `HandlerMethodValidationException` handler removed (Spring 6 only at the time)

> All of the above were reversed by the OpenSearch migration completed on 2026-06-16.
