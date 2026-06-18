# Contributing — aatrika-entity

Developer guide for working on, maintaining, and releasing this service.

---

## Table of Contents

- [Development Setup](#development-setup)
- [Claude Code Setup](#claude-code-setup)
- [Scripts Reference](#scripts-reference)
  - [check-triage.sh](#check-triagesh)
  - [update-release-notes.sh](#update-release-notessh)
- [Release Process](#release-process)
- [Commit Guidelines](#commit-guidelines)
- [Key Documents](#key-documents)

---

## Development Setup

### Prerequisites

- Java 21
- Maven 3.8+
- PostgreSQL running on `localhost:5433` (database: `aastrika_entity_es7`)
- OpenSearch running on `localhost:9200`

### Build and run

```bash
# Build (skip tests)
./mvnw clean package -DskipTests

# Run
./mvnw spring-boot:run

# Run tests
./mvnw test
```

### IDE setup

The project uses **Google Java Format** (2-space indent, 100-char line limit). Configure your IDE formatter to match or run `./mvnw fmt:check` to verify before committing.

---

## Claude Code Setup

If you use Claude Code as your AI assistant, one additional setup step is required to enforce the hard rule that no AI agent should run git write commands.

`.claude/settings.json` is gitignored (personal tool config) — create it manually in the project root:

```json
{
  "hooks": {
    "PreToolUse": [
      {
        "matcher": "Bash",
        "hooks": [
          {
            "type": "command",
            "command": "bash scripts/guard-git-writes.sh"
          }
        ]
      }
    ]
  }
}
```

This wires `scripts/guard-git-writes.sh` as a pre-execution guard. Whenever Claude attempts a Bash tool call containing a git write command (`commit`, `push`, `tag`, `merge`, `rebase`, `reset`, etc.), the call is **blocked** before it runs and Claude is shown the command to suggest to you instead.

**Performance:** The hook runs only on Bash tool calls (~5–10ms each). It has no impact on conversational responses, file reads, or edits.

**Non-Claude Code users:** Skip this section entirely. The hook has no effect without Claude Code.

---

## Scripts Reference

All scripts live in `scripts/` and are run from the project root.

---

### check-triage.sh

Verifies all automatable items in `docs/TRIAGE.md` against the actual codebase and reports which are resolved and which remain open.

#### Usage

```bash
# Report only — no file changes
bash scripts/check-triage.sh

# Report + auto-update the Status column in TRIAGE.md for resolved items
bash scripts/check-triage.sh --update
```

#### What it checks

| ID | Item | How |
|---|---|---|
| T-001 | Tomcat CVE patch | `<tomcat.version>` value in `pom.xml` |
| T-002 | OpenSearch migration | `spring-data-opensearch` dependency in `pom.xml` |
| T-003 | `ddl-auto` default | Default value in `application.properties` |
| T-004 | `JsonMapConverter.java` deleted | File existence check |
| T-006 | Commented code in readers | Grep for known commented patterns |
| T-007 | Commented query in repository | Grep for commented lines |
| T-008 | OpenSearch debug logging guarded | Hardcoded `DEBUG` check in properties |
| T-010 | Controller injects interface | Grep for impl class in controller |
| T-012 | `CompetencyLevelResponseDTO.id` | Grep for commented-out field |
| T-013 | OpenSearch credentials enforced | Empty default check in properties |
| T-014 | DB error message sanitised | Grep for `rootCause.getMessage()` in exception handler |

Items requiring a live database (T-005) or human judgment (T-009, T-011) are flagged as `⚠ SKIP` with a manual check instruction printed inline.


---

### update-release-notes.sh

Drafts a new CHANGELOG entry using `docs/RELEASE_NOTE_TEMPLATE.md` as the structure, populated with commits from git log since the last release.

#### Usage

```bash
# Preview draft — no file changes
bash scripts/update-release-notes.sh <version>

# Prepend draft to CHANGELOG.md
bash scripts/update-release-notes.sh <version> --apply
```

#### Example

```bash
bash scripts/update-release-notes.sh 0.7.0
bash scripts/update-release-notes.sh 0.7.0 --apply
```

#### How it works

1. Reads the date of the last entry in `CHANGELOG.md`
2. Collects all non-merge commits since that date via `git log`
3. Categorises commits by keyword into sections: Security, Added, Changed, Fixed, Removed
4. Injects them into `docs/RELEASE_NOTE_TEMPLATE.md`, replacing placeholder bullets
5. Outputs the draft to stdout — or prepends to `CHANGELOG.md` with `--apply`

> The draft is a starting point — always review and edit before committing.
> Remove all `<!-- instructional comment -->` blocks before publishing.

---

## Release Process

Follow these steps when cutting a release:

### 1. Verify triage status

```bash
bash scripts/check-triage.sh
```

Review open items. Resolve any `[security]`-tagged items before releasing.

### 2. Draft the release note

```bash
bash scripts/update-release-notes.sh <version> --apply
```

Open `CHANGELOG.md` and:
- Fill in the **Summary** section
- Review auto-categorised commit items — move or reword as needed
- Fill in **Migration Notes** (env var changes, DB scripts, config changes)
- Tick off the **Deployment Checklist** during deployment

The template structure is in `docs/RELEASE_NOTE_TEMPLATE.md` — refer to it for section guidance.

### 3. Update TRIAGE.md

```bash
bash scripts/check-triage.sh --update
```

Mark any newly resolved items as completed.

### 4. Commit and tag

Run these yourself — no AI agent should or will run git commands on your behalf:

```bash
git add CHANGELOG.md docs/TRIAGE.md
git commit -m "Release v<version>"
git tag v<version>
```

### 5. Deploy and validate

Work through the Deployment Checklist in the CHANGELOG entry:
- DB migration scripts executed
- Env vars updated in deployment config
- `JPA_DDL_AUTO=validate` confirmed in production
- `OPENSEARCH_USERNAME` / `OPENSEARCH_PASSWORD` set
- Application connects to OpenSearch, Swagger UI loads, fuzzy search returns results

---

## Commit Guidelines

This project does not enforce conventional commits, but keeping messages descriptive helps the release note script categorise them accurately.

| Prefix pattern | Maps to section |
|---|---|
| `add`, `create`, `implement`, `new` | Added |
| `update`, `change`, `migrate`, `replace`, `refactor` | Changed |
| `fix`, `correct`, `resolve`, `repair` | Fixed |
| `remove`, `delete`, `drop`, `clean` | Removed |
| `cve`, `security`, `patch`, `credential` | Security |

Commits that don't match any pattern land in **Other** and need manual review in the draft.

---

## Key Documents

| File | Purpose |
|---|---|
| `CLAUDE.md` | AI coding rules — stack, conventions, do-nots |
| `docs/DECISIONS.md` | Project decisions and lessons — read before implementing |
| `SECURITY.md` | Security scope, rules, and release notes |
| `docs/TRIAGE.md` | Full issue backlog with severity and status |
| `CHANGELOG.md` | Release history from inception |
| `docs/RELEASE_NOTE_TEMPLATE.md` | Template structure for every release note |
