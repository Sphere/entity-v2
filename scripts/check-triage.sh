#!/usr/bin/env bash
# check-triage.sh — verify automatable TRIAGE.md items against actual codebase
# Usage:
#   bash scripts/check-triage.sh          # report only
#   bash scripts/check-triage.sh --update # report + update TRIAGE.md Action Priority table

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
TRIAGE="$ROOT/docs/TRIAGE.md"
UPDATE=false
[[ "${1:-}" == "--update" ]] && UPDATE=true

# Colours
GREEN="\033[0;32m"
RED="\033[0;31m"
YELLOW="\033[0;33m"
RESET="\033[0m"
BOLD="\033[1m"

pass() { echo -e "  ${GREEN}✅ DONE${RESET}  $1"; }
fail() { echo -e "  ${RED}⬜ OPEN${RESET}  $1"; }
skip() { echo -e "  ${YELLOW}⚠  SKIP${RESET}  $1 (needs manual check)"; }

declare -A STATUSES   # item → "done" | "open" | "skip"

echo ""
echo -e "${BOLD}=== Triage Status Check — $(date '+%Y-%m-%d') ===${RESET}"
echo ""

# ── T-001: Tomcat CVE patch ────────────────────────────────────────────────
echo -e "${BOLD}T-001${RESET} Tomcat CVE patch"
if grep -q '<tomcat.version>10\.' "$ROOT/pom.xml" 2>/dev/null; then
  TOMCAT_VER=$(grep -o '<tomcat.version>[^<]*' "$ROOT/pom.xml" | grep -o '[0-9][^<]*')
  pass "tomcat.version=$TOMCAT_VER found in pom.xml"
  STATUSES["T-001"]="done"
else
  fail "<tomcat.version> override with 10.x not found in pom.xml"
  STATUSES["T-001"]="open"
fi

# ── T-002: OpenSearch migration ────────────────────────────────────────────
echo -e "${BOLD}T-002${RESET} OpenSearch migration"
if grep -q 'spring-data-opensearch' "$ROOT/pom.xml" 2>/dev/null; then
  pass "spring-data-opensearch dependency present in pom.xml"
  STATUSES["T-002"]="done"
else
  fail "spring-data-opensearch not found — ES migration incomplete"
  STATUSES["T-002"]="open"
fi

# ── T-003: ddl-auto default ────────────────────────────────────────────────
echo -e "${BOLD}T-003${RESET} ddl-auto default safety"
PROPS="$ROOT/src/main/resources/application.properties"
if grep -qP 'ddl-auto=\$\{[^}]+:update\}' "$PROPS" 2>/dev/null; then
  fail "ddl-auto defaults to 'update' — change default to 'validate' in $PROPS"
  STATUSES["T-003"]="open"
else
  pass "ddl-auto default is not 'update'"
  STATUSES["T-003"]="done"
fi

# ── T-004: JsonMapConverter.java deleted ──────────────────────────────────
echo -e "${BOLD}T-004${RESET} JsonMapConverter.java deleted"
if find "$ROOT/src" -name "JsonMapConverter.java" | grep -q .; then
  fail "JsonMapConverter.java still exists — delete it"
  STATUSES["T-004"]="open"
else
  pass "JsonMapConverter.java not found — already deleted"
  STATUSES["T-004"]="done"
fi

# ── T-005: additional_properties DB column (needs DB — skipped) ───────────
echo -e "${BOLD}T-005${RESET} additional_properties DB column drop"
skip "requires live DB connection — check manually: SELECT column_name FROM information_schema.columns WHERE table_name='master_entities' AND column_name='additional_properties';"
STATUSES["T-005"]="skip"

# ── T-006: Commented code in sheet readers ────────────────────────────────
echo -e "${BOLD}T-006${RESET} Commented-out code in sheet readers"
CSV_HITS=$(grep -c '//.*\(additionalProperties\|updatedDate\|createdDate\)' \
  "$ROOT/src/main/java/com/aastrika/entity/reader/CsvEntitySheetReader.java" 2>/dev/null || true)
XLSX_HITS=$(grep -c '//.*\(additionalProperties\|@Override\)' \
  "$ROOT/src/main/java/com/aastrika/entity/reader/XlsxEntitySheetReader.java" 2>/dev/null || true)
if [[ "$CSV_HITS" -gt 0 || "$XLSX_HITS" -gt 0 ]]; then
  fail "Commented-out code still present (CsvEntitySheetReader: $CSV_HITS, XlsxEntitySheetReader: $XLSX_HITS)"
  STATUSES["T-006"]="open"
else
  pass "No commented-out blocks found in sheet readers"
  STATUSES["T-006"]="done"
fi

# ── T-007: Commented-out query in MasterEntityRepository ─────────────────
echo -e "${BOLD}T-007${RESET} Commented-out query in MasterEntityRepository"
REPO="$ROOT/src/main/java/com/aastrika/entity/repository/jpa/MasterEntityRepository.java"
if grep -q '^\s*//' "$REPO" 2>/dev/null; then
  fail "Commented-out lines still present in MasterEntityRepository.java"
  STATUSES["T-007"]="open"
else
  pass "No commented-out lines in MasterEntityRepository.java"
  STATUSES["T-007"]="done"
fi

# ── T-008: ES debug logging guarded ──────────────────────────────────────
echo -e "${BOLD}T-008${RESET} OpenSearch debug logging guarded"
if grep -qP 'logging\.level\.org\.springframework\.data\.elasticsearch=DEBUG' "$PROPS" 2>/dev/null; then
  fail "Elasticsearch logging hardcoded to DEBUG — guard behind env var"
  STATUSES["T-008"]="open"
else
  pass "Elasticsearch logging is not hardcoded to DEBUG"
  STATUSES["T-008"]="done"
fi

# ── T-009: getAllHeaders() unused (semantic — skipped) ────────────────────
echo -e "${BOLD}T-009${RESET} getAllHeaders() unused method"
CALLERS=$(grep -rl 'getAllHeaders()' "$ROOT/src" --include="*.java" 2>/dev/null | grep -v 'EntitySheetProperties.java' || true)
if [[ -z "$CALLERS" ]]; then
  skip "getAllHeaders() has no callers — but decision to wire or remove needs human judgment"
  STATUSES["T-009"]="skip"
else
  pass "getAllHeaders() is now called from: $CALLERS"
  STATUSES["T-009"]="done"
fi

# ── T-010: EntityMappingController injects impl ───────────────────────────
echo -e "${BOLD}T-010${RESET} EntityMappingController interface injection"
CTRL="$ROOT/src/main/java/com/aastrika/entity/controller/EntityMappingController.java"
if grep -q 'EntityMappingServiceImpl' "$CTRL" 2>/dev/null; then
  fail "EntityMappingController still injects EntityMappingServiceImpl — change to EntityMappingService interface"
  STATUSES["T-010"]="open"
else
  pass "EntityMappingController injects the interface"
  STATUSES["T-010"]="done"
fi

# ── T-011: language param wired (semantic — skipped) ─────────────────────
echo -e "${BOLD}T-011${RESET} language param in upload endpoint"
skip "requires code review to confirm whether param is intentionally unused or should be wired through"
STATUSES["T-011"]="skip"

# ── T-012: CompetencyLevelResponseDTO.id commented out ───────────────────
echo -e "${BOLD}T-012${RESET} CompetencyLevelResponseDTO.id commented out"
DTO="$ROOT/src/main/java/com/aastrika/entity/dto/response/CompetencyLevelResponseDTO.java"
if grep -q '//.*private.*id\|//.*Integer.*id\|//.*int.*id' "$DTO" 2>/dev/null; then
  fail "id field is commented out in CompetencyLevelResponseDTO — delete or restore it"
  STATUSES["T-012"]="open"
else
  pass "No commented-out id field in CompetencyLevelResponseDTO"
  STATUSES["T-012"]="done"
fi

# ── T-013: OpenSearch empty credentials ──────────────────────────────────
echo -e "${BOLD}T-013${RESET} OpenSearch credentials enforced [security]"
if grep -qP 'opensearch\.username=\$\{[^}]+:\}' "$PROPS" 2>/dev/null; then
  fail "OpenSearch username defaults to empty — enforce non-empty credentials in production"
  STATUSES["T-013"]="open"
else
  pass "OpenSearch username does not default to empty"
  STATUSES["T-013"]="done"
fi

# ── T-014: DataIntegrityViolationException exposes root cause ─────────────
echo -e "${BOLD}T-014${RESET} DataIntegrityViolationException sanitised [security]"
HANDLER="$ROOT/src/main/java/com/aastrika/entity/config/ApplicationExceptionHandler.java"
if grep -q 'rootCause.getMessage()' "$HANDLER" 2>/dev/null; then
  fail "Raw PostgreSQL root cause message returned in API response — sanitise before exposing"
  STATUSES["T-014"]="open"
else
  pass "DataIntegrityViolationException handler does not expose raw root cause"
  STATUSES["T-014"]="done"
fi

# ── Summary ───────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}=== Summary ===${RESET}"
DONE_COUNT=0; OPEN_COUNT=0; SKIP_COUNT=0
for id in T-001 T-002 T-003 T-004 T-005 T-006 T-007 T-008 T-009 T-010 T-011 T-012 T-013 T-014; do
  case "${STATUSES[$id]}" in
    done) DONE_COUNT=$((DONE_COUNT + 1)) ;;
    open) OPEN_COUNT=$((OPEN_COUNT + 1)) ;;
    skip) SKIP_COUNT=$((SKIP_COUNT + 1)) ;;
  esac
done
echo -e "  ${GREEN}Completed: $DONE_COUNT${RESET} | ${RED}Open: $OPEN_COUNT${RESET} | ${YELLOW}Needs manual check: $SKIP_COUNT${RESET}"
echo ""

# ── Optional TRIAGE.md update ─────────────────────────────────────────────
if [[ "$UPDATE" == true ]]; then
  echo -e "${BOLD}Updating TRIAGE.md Action Priority table...${RESET}"
  TODAY=$(date '+%Y-%m-%d')

  for id in "${!STATUSES[@]}"; do
    STATUS="${STATUSES[$id]}"
    if [[ "$STATUS" == "done" ]]; then
      # Replace "| Open |" with completed date on the row containing this ID
      # Match the T-XXX ID at the start of a table row (handles [security] tag in same cell)
    sed -i "/| $id /s/| Open |/| ✅ Completed $TODAY |/" "$TRIAGE"
    fi
    # "skip" rows are left unchanged — they stay Open until manually reviewed
  done

  echo -e "  ${GREEN}TRIAGE.md updated.${RESET}"
  echo ""
fi