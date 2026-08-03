# Changelog — aatrika-entity

Version index for all releases.
Full release notes (migration steps, deployment checklist, known issues) are in `docs/release-notes/` from v0.6.0 onwards.

---

## [0.9.0] — 2026-08-03 · [Full Release Note](docs/release-notes/v0.9.0.md)

Test coverage and static analysis baseline. Coverage raised from 37% to 88% and SonarQube reporting wired into the Maven build. No application code changed.

- **Added:** `jacoco-maven-plugin 0.8.15` — coverage report generated at both `test` and `verify` phases
- **Added:** `sonar-maven-plugin 5.7.0.6970` (pinned) and `sonar.*` properties in `pom.xml`; token supplied at run time, never stored in source
- **Added:** `lombok.config` with `addLombokGeneratedAnnotation` — keeps Lombok-generated members out of the coverage denominator
- **Added:** 133 tests across the `mapper`, `reader`, `util` and `controller` packages (42 → 175 total)
- **Added:** `EntityTypeExtension` and `TestApplicationProperties` test support — bind `entity.entityTypeList` and `entity-sheet.*` from the deployed `application.properties` so tests cannot drift from config
- **Fixed:** 16 pre-existing test failures — `EntityType`'s static type set is populated only at Spring startup, leaving it empty in Mockito-only tests
- **Fixed:** OpenSearch search tests asserted only the mapped response, never the query built — the `strict`, blank-language and blank-query branches were unverifiable
- **Changed:** `.gitignore` — ignore `htmlReport/` and `claude-kit/`

Coverage: 88.2% (91.4% line, 79.0% branch) · Quality gate: OK · 0 bugs, 0 vulnerabilities, 0 security hotspots

---

## [0.8.0] — 2026-07-02 · [Full Release Note](docs/release-notes/v0.8.0.md)

Completes the OpenSearch integration and expands the entity model to the full organisational and geographic hierarchy. Entity types become runtime configuration. YugabyteDB compatibility added.

- **Added:** `entity.entityTypeList` property — valid entity types loaded at startup via `EntityStartupApplicationRunner`; adding a type is now a config change, not a code change
- **Added:** Six entity types — `ORGANIZATION`, `STATE`, `DISTRICT`, `BLOCK`, `FACILITY`, `POSTING_FACILITY` — enabling geo/org hierarchy modelling
- **Added:** `opensearch.index.master-entities` — OpenSearch index name configurable per environment via `OPENSEARCH_INDEX_MASTER_ENTITIES`
- **Changed:** `spring-data-opensearch-starter` `1.5.3 → 1.6.0` — resolves `NoSuchMethodError` on search endpoints caused by Spring Boot 3.4.2 pulling `spring-data-elasticsearch 5.4.x`
- **Changed:** `EntityType` refactored from Java `enum` to `final class` with a runtime-loaded type set; only `EntityType.COMPETENCY` remains a compile-time constant, all other types validated via `EntityType.validate(String)`
- **Changed:** `entity-map.allowed-type-combinations` expanded from 3 to 9 combinations covering `ORGANIZATION → POSITION → ROLE → ACTIVITY → COMPETENCY` and `STATE → DISTRICT → BLOCK → FACILITY/POSTING_FACILITY`
- **Changed:** `saveEntityDetailsInES` takes `userId` as a third parameter — sets `createdBy` and `createdAt` on each OpenSearch document
- **Changed:** `EntitySheetMappingValidator` renamed to `EntityStartupApplicationRunner`, now also loading entity types at startup
- **Changed:** ⚠ **Breaking default** — `spring.jpa.hibernate.ddl-auto` default `update → none`; `JPA_DDL_AUTO` must now be set explicitly per environment
- **Changed:** HikariCP pool tuned for YugabyteDB stability; JDBC URL carries `socketTimeout=60`
- **Fixed:** Test suite compilation failures on Jenkins caused by the `EntityType` enum-to-class refactor not being propagated to test classes

---

## [0.7.0] — 2026-06-18 · [Full Release Note](docs/release-notes/v0.7.0.md)

Developer governance and AI guardrails. Hard enforcement blocking AI agents from running git commands. Documentation reorganised for team-wide clarity.

- **Added:** `scripts/guard-git-writes.sh` — PreToolUse hook blocking git write commands from AI agents
- **Added:** `docs/DECISIONS.md` — renamed from `docs/lesson.md`; project decisions useful to all developers
- **Changed:** `CLAUDE.md` — hard no-git rule added; non-Claude developer note; `DECISIONS.md` reference updated
- **Changed:** `CONTRIBUTING.md` — Claude Code Setup section; release process git step clarified; stale Stop hook note removed
- **Changed:** `README.md` — stale Elasticsearch reference fixed; Postman collection linked

---

## [0.6.0] — 2026-06-16 · [Full Release Note](docs/release-notes/v0.6.0.md)

OpenSearch migration complete. Spring Boot restored to 3.4.2 + Java 21. Tomcat CVEs patched. New search capabilities and CSV safeguards added. Security and developer tooling baseline established.

- **Security:** Patched CVE-2025-24813, CVE-2025-31651, CVE-2025-55754 (Tomcat 10.1.36)
- **Added:** Fuzzy phrase search, phrase search, dynamic filtered search (entity type / language / field, strict or fuzzy mode)
- **Added:** Configurable CSV header-field-mappings in `application.properties` — rename sheet columns without code changes
- **Added:** Duplicate entry check before bulk upload — rejects sheet rows that already exist in DB
- **Changed:** Elasticsearch → OpenSearch (`spring-data-opensearch-starter 1.6.0`); `javax.*` → `jakarta.*`; Spring Boot `2.7.18 → 3.4.2`; Java `17 → 21`
- **Removed:** `JsonMapConverter.java`; `Serializable` from JPA models
- **Added:** `SECURITY.md`, `docs/TRIAGE.md`, `CONTRIBUTING.md`, `scripts/check-triage.sh`, `scripts/update-release-notes.sh`
- **Fixed:** `CLAUDE.md` had 8 conflicts with actual codebase — all resolved

---

## [0.5.0] — 2026-03-12

Full hierarchy tree API with language fallback. Multi-entity update. Mapping config validation.

- **Added:** `POST /v1/entity/hierarchy` — full parent-child tree with English fallback for missing language variants
- **Added:** Mapping config validation via `entity-map.allowed-type-combinations` property
- **Changed:** `PUT /v1/entity/update` now accepts a list; hierarchy fetch uses bulk DB query (no per-node calls)
- **Fixed:** Hierarchy language fallback; mapping create/update/search edge cases

---

## [0.4.0] — 2026-02-26

Delete API. EntityType enum for type safety.

- **Added:** `DELETE /v1/entity/delete` — single-language delete and full purge (`purgeAllLanguage: true`)
- **Added:** `EntityType` enum — replaces raw strings across all entity type references
- **Changed:** Delete preserves mappings if other language variants exist; removes mappings only on last variant

---

## [0.3.0] — 2026-02-20

Mapping API. Unified response and error handling.

- **Added:** `POST /v1/entity/mapping` — create, update, search parent-child relationships
- **Added:** `AppResponse` wrapper — consistent response envelope across all endpoints
- **Added:** `ApplicationExceptionHandler` — unified error responses for all exception types
- **Fixed:** Response and error format standardised; test suite passing

---

## [0.2.0] — 2026-02-10

CI/CD setup. Entity update and search.

- **Added:** `Dockerfile`, `Jenkinsfile`, `build.sh` — containerisation and CI/CD pipeline
- **Added:** `PUT /v1/entity/update`, `GET /v1/entity/search` endpoints
- **Changed:** Configuration migrated from `application.yaml` to `application.properties`

---

## [0.1.0] — 2026-02-05

Initial release.

- **Added:** Spring Boot service skeleton for master data management (Competencies, Roles, Activities, Positions)
- **Added:** CSV sheet upload with header validation and field mapping
- **Added:** Fuzzy name search via Elasticsearch
- **Added:** `MasterEntity` + `CompetencyLevel` JPA models; MapStruct mappers; `AppResponse` foundation
- **Added:** Google Java Format style (2-space indent, 100-char line limit)
