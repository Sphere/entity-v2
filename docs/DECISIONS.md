# Decisions — aatrika-entity

Rules and lessons learned from real mistakes in this project. Read these before implementing anything.

---

## L-001 — Verify CLAUDE.md against the actual code before enforcing any rule

**Mistake:** CLAUDE.md said `javax.*` imports, Hibernate 5, Spring Boot 2.7.18. The codebase had already migrated to `jakarta.*`, Hibernate 6, Spring Boot 3.4.2. Claude enforced the stale rules.

**Rule:** Before writing new code or reviewing existing code, grep the actual files to confirm what is in use. Never trust a rule in CLAUDE.md without checking the code first. If a rule conflicts with what the code actually does, stop and flag the conflict — do not silently pick one.

---

## L-002 — When a migration is executed, update CLAUDE.md immediately

**Mistake:** The OpenSearch migration was completed (dependency swapped, imports changed, config updated) but CLAUDE.md still described it as "pending" and contained the old rules. This caused every subsequent code generation to use the wrong baseline.

**Rule:** CLAUDE.md is only useful if it reflects the current state. After any significant change — dependency upgrade, migration, breaking refactor — the rules section, tech stack table, and pending work table must all be updated in the same PR.

---

## L-003 — Release notes and "Do NOT" rules must be pruned when the condition no longer applies

**Mistake:** CLAUDE.md had a "Do NOT use HandlerMethodValidationException" rule that was correct for Spring Boot 2.7 / Spring 5 but wrong for Spring Boot 3 / Spring 6. The rule was never removed after the upgrade. Same issue with "must implement Serializable" — correct for Hibernate 5.6, wrong for Hibernate 6.

**Rule:** Every "Do NOT" rule must have a condition. When the condition changes (e.g. framework version upgrade), revisit all "Do NOT" entries and remove or rewrite any that no longer apply.

---

## L-004 — "Removed" in release notes does not mean removed if the code says otherwise

**Mistake:** CLAUDE.md release notes stated `additional_properties` was removed from `MasterEntity`. The field was still present in `MasterEntity.java` with `@JdbcTypeCode(SqlTypes.JSON)`. Claude treated the note as truth without verifying.

**Rule:** Release notes describe intent, not ground truth. Always read the actual source file to confirm a stated change was applied. If the note and the code disagree, the code is the source of truth.

---

## L-005 — Dead rules about deleted files cause confusion

**Mistake:** CLAUDE.md had a rule "Do not delete JsonMapConverter.java until confirmed no other module references it." The file was already deleted. The rule lingered and created false caution.

**Rule:** When a file is deleted, remove all rules that reference it. A rule guarding a file that does not exist cannot be acted on and misleads future sessions.

---

## L-006 — Security and triage documents need a reference in CLAUDE.md to be AI-enforced

**Learning:** Files like `SECURITY.md` and `TRIAGE.md` are not loaded automatically by Claude Code. Only `CLAUDE.md` is always loaded. Without an explicit directive in `CLAUDE.md` to read them, they are human-facing documents only — Claude will not apply their rules.

**Rule:** Any document whose rules Claude should enforce must be referenced in `CLAUDE.md` with a clear instruction such as: "Before security-related work, read `SECURITY.md` for scope and policy."

---

## L-007 — Flag conflicts before implementing, never resolve them silently

**Learning:** When CLAUDE.md contains a rule that contradicts what the codebase actually does, silently picking one causes incorrect output and erodes trust.

**Rule:** If you observe a conflict between a rule in `CLAUDE.md` and the actual state of the code, stop and tell the user explicitly before writing any code. State: what the rule says, what the code shows, and which is likely correct. Let the user decide.
