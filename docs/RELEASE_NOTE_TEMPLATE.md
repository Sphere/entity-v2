# Release Note Template — aatrika-entity

> Copy this file for each release. Fill in each section, remove sections that don't apply,
> and delete all instructional comments before publishing.

---

## [VERSION] — YYYY-MM-DD

### Summary
<!-- One or two sentences: what is this release and why does it matter? -->
<!-- Example: Introduces OpenSearch migration and restores Spring Boot 3.x baseline. -->

---

### Security
<!-- CVE patches, dependency upgrades driven by vulnerabilities, auth/credential changes.
     Always include CVE IDs where applicable. -->
- [ ] Item

---

### Added
<!-- New features, endpoints, config options, or capabilities introduced in this release. -->
- [ ] Item

---

### Changed
<!-- Modifications to existing behaviour, API contracts, config defaults, or dependencies.
     Clearly state what changed FROM and TO where relevant (e.g. "Spring Boot 2.7.18 → 3.4.2"). -->
- [ ] Item

---

### Fixed
<!-- Bug fixes. Reference TRIAGE.md item ID (e.g. T-003) if applicable. -->
- [ ] Item

---

### Removed
<!-- Deleted files, dropped columns, deprecated endpoints removed, dependencies removed. -->
- [ ] Item

---

### Known Issues
<!-- Open items NOT resolved in this release. Reference TRIAGE.md IDs. -->
- [ ] Item (see TRIAGE.md T-XXX)

---

### Migration Notes
<!-- Steps required when upgrading from the previous version.
     Include: DB migration scripts, env var changes, config renames, breaking API changes. -->

#### Environment Variables
<!-- List any new, renamed, or removed env vars. -->
| Variable | Change | Action Required |
|---|---|---|
| `EXAMPLE_VAR` | Added | Set in deployment config |

#### Database
<!-- SQL scripts to run, if any. -->
```sql
-- Example: ALTER TABLE master_entities DROP COLUMN IF EXISTS additional_properties;
```

#### Config Changes
<!-- Properties added, renamed, or removed in application.properties. -->
```properties
# Example:
# Before:
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:update}
# After:
spring.jpa.hibernate.ddl-auto=${JPA_DDL_AUTO:validate}
```

---

### Deployment Checklist
<!-- Tick each item before marking this release as deployed. -->
- [ ] DB migration scripts executed
- [ ] Environment variables updated in deployment config
- [ ] `JPA_DDL_AUTO` set to `validate` (not `update`) in production
- [ ] `OPENSEARCH_USERNAME` and `OPENSEARCH_PASSWORD` set in production
- [ ] Application starts and connects to OpenSearch
- [ ] Swagger UI loads at `/swagger-ui/index.html`
- [ ] Fuzzy name search returns results
- [ ] Entity create / update / delete syncs to OpenSearch
- [ ] `mvn dependency:tree | grep tomcat-embed-core` confirms Tomcat patch version active
- [ ] TRIAGE.md updated — run `bash scripts/check-triage.sh --update`

---

### Contributors
<!-- Authors who contributed to this release. -->
- @rkrahu
