#!/usr/bin/env bash
# update-release-notes.sh — draft a release note using RELEASE_NOTE_TEMPLATE.md,
#                            populated from git log since the last release.
#
# Usage:
#   bash scripts/update-release-notes.sh <version>           # draft to stdout only
#   bash scripts/update-release-notes.sh <version> --apply   # write release note file
#                                                             # + prepend entry to CHANGELOG.md
#
# Output (--apply):
#   docs/release-notes/v{version}.md   ← full release note from template (review and fill in)
#   CHANGELOG.md                       ← short index entry prepended (version + bullets only)
#
# Examples:
#   bash scripts/update-release-notes.sh 0.7.0
#   bash scripts/update-release-notes.sh 0.7.0 --apply

set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
CHANGELOG="$ROOT/CHANGELOG.md"
TEMPLATE="$ROOT/docs/RELEASE_NOTE_TEMPLATE.md"
RELEASE_NOTES_DIR="$ROOT/docs/release-notes"
VERSION="${1:-}"
APPLY=false
[[ "${2:-}" == "--apply" ]] && APPLY=true

# ── Validate ──────────────────────────────────────────────────────────────
if [[ -z "$VERSION" ]]; then
  echo "Usage: bash scripts/update-release-notes.sh <version> [--apply]"
  echo "Example: bash scripts/update-release-notes.sh 0.7.0 --apply"
  exit 1
fi

if [[ ! "$VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "Error: version must be in X.Y.Z format (e.g. 0.7.0)"
  exit 1
fi

if [[ ! -f "$TEMPLATE" ]]; then
  echo "Error: RELEASE_NOTE_TEMPLATE.md not found at $TEMPLATE"
  exit 1
fi

TODAY=$(date '+%Y-%m-%d')

# ── Find the last release date from CHANGELOG.md ──────────────────────────
LAST_VERSION=""
LAST_DATE=""
if [[ -f "$CHANGELOG" ]]; then
  LAST_VERSION=$(grep -oP '## \[\K[^\]]+' "$CHANGELOG" | head -1)
  LAST_DATE=$(grep -oP '## \[\d+\.\d+\.\d+\] — \K\d{4}-\d{2}-\d{2}' "$CHANGELOG" | head -1)
fi

if [[ -n "$LAST_DATE" ]]; then
  echo "ℹ  Last release: v$LAST_VERSION on $LAST_DATE"
  echo "ℹ  Reading commits since $LAST_DATE"
  SINCE_ARG="--after=$LAST_DATE"
else
  echo "ℹ  No previous release found — reading all commits"
  SINCE_ARG=""
fi

# ── Collect and categorise commits ────────────────────────────────────────
COMMITS=$(git -C "$ROOT" log $SINCE_ARG \
  --pretty=format:"%s (%h)" \
  --no-merges \
  --reverse 2>/dev/null)

if [[ -z "$COMMITS" ]]; then
  echo "⚠  No new commits found since $LAST_DATE."
  echo "   Opening template with empty sections for manual fill-in."
fi

ADDED=""; CHANGED=""; FIXED=""; SECURITY=""; REMOVED=""; OTHER=""

while IFS= read -r line; do
  [[ -z "$line" ]] && continue
  lower=$(echo "$line" | tr '[:upper:]' '[:lower:]')
  entry="- $line"
  if echo "$lower" | grep -qE 'cve|secur|vulnerab|credential|sanitise|sanitize|patch.*cve'; then
    SECURITY="${SECURITY}${entry}\n"
  elif echo "$lower" | grep -qE '\badd\b|creat|implement|\bnew\b|introduc|support'; then
    ADDED="${ADDED}${entry}\n"
  elif echo "$lower" | grep -qE '\bfix\b|correct|resolv|repair'; then
    FIXED="${FIXED}${entry}\n"
  elif echo "$lower" | grep -qE 'remov|delet|drop|clean'; then
    REMOVED="${REMOVED}${entry}\n"
  elif echo "$lower" | grep -qE 'updat|chang|migrat|replac|refactor|improv|upgrad|revert'; then
    CHANGED="${CHANGED}${entry}\n"
  else
    OTHER="${OTHER}${entry}\n"
  fi
done <<< "$COMMITS"

# ── Build draft from template structure ───────────────────────────────────
# Replace VERSION and DATE placeholders
DRAFT=$(sed \
  -e "s/\[VERSION\]/[$VERSION]/g" \
  -e "s/YYYY-MM-DD/$TODAY/g" \
  "$TEMPLATE")

# Inject git commits into matching sections (after the section heading line)
inject_section() {
  local section="$1"
  local items="$2"
  local placeholder="- \[ \] Item"

  if [[ -n "$items" ]]; then
    # Replace the placeholder bullet with the actual items
    DRAFT=$(echo "$DRAFT" | awk \
      -v section="### ${section}" \
      -v items="$(echo -e "$items" | sed 's/$/\\n/' | tr -d '\n')" \
      -v placeholder="- \[ \] Item" '
      /^### /{in_section=0}
      $0 ~ section {in_section=1}
      in_section && $0 ~ placeholder {
        gsub(/\\n$/, "", items)
        printf "%s\n", items
        in_section=0
        next
      }
      {print}
    ')
  fi
}

inject_section "Security"  "$SECURITY"
inject_section "Added"     "$ADDED"
inject_section "Changed"   "$CHANGED"
inject_section "Fixed"     "$FIXED"
inject_section "Removed"   "$REMOVED"

# Append any unclassified commits to a Note at the bottom of Summary
if [[ -n "$OTHER" ]]; then
  DRAFT=$(echo "$DRAFT" | sed "/^### Summary/a\\\\n> Unclassified commits (review manually):\\n$(echo -e "$OTHER" | sed 's/$/\\n/' | tr -d '\n')")
fi

# ── Output ────────────────────────────────────────────────────────────────
echo ""
echo "=========================================="
echo "  Draft release note — v$VERSION ($TODAY)"
echo "  Based on: RELEASE_NOTE_TEMPLATE.md"
echo "=========================================="
echo ""
echo "$DRAFT"

if [[ "$APPLY" == true ]]; then
  RELEASE_NOTE_FILE="$RELEASE_NOTES_DIR/v${VERSION}.md"

  # ── Guard: don't overwrite an existing release note ───────────────────
  if [[ -f "$RELEASE_NOTE_FILE" ]]; then
    echo "⚠  $RELEASE_NOTE_FILE already exists — will not overwrite."
    echo "   Delete it manually if you want to regenerate."
    exit 1
  fi

  mkdir -p "$RELEASE_NOTES_DIR"

  # ── 1. Write full release note file (from template) ───────────────────
  echo "$DRAFT" > "$RELEASE_NOTE_FILE"
  echo ""
  echo "✅ Release note written → $RELEASE_NOTE_FILE"

  # ── 2. Build short CHANGELOG index entry ──────────────────────────────
  # Format: version header + link + one-line placeholder summary + bullet points
  CHANGELOG_ENTRY="## [$VERSION] — $TODAY · [Full Release Note](docs/release-notes/v${VERSION}.md)\n"
  CHANGELOG_ENTRY="${CHANGELOG_ENTRY}\n<!-- Add one-line summary here -->\n"

  for section in Security Added Changed Fixed Removed; do
    var_name=$(echo "$section" | tr '[:lower:]' '[:upper:]')
    # Dynamically get the variable value by section name
    case $section in
      Security) items="$SECURITY" ;;
      Added)    items="$ADDED" ;;
      Changed)  items="$CHANGED" ;;
      Fixed)    items="$FIXED" ;;
      Removed)  items="$REMOVED" ;;
    esac
    if [[ -n "$items" ]]; then
      while IFS= read -r line; do
        [[ -z "$line" ]] && continue
        CHANGELOG_ENTRY="${CHANGELOG_ENTRY}- **${section}:** ${line#- }\n"
      done <<< "$(echo -e "$items")"
    fi
  done

  # ── 3. Prepend short entry to CHANGELOG.md ────────────────────────────
  HEADER=$(awk '/^## \[/{exit} {print}' "$CHANGELOG")
  BODY=$(awk '/^## \[/{found=1} found{print}' "$CHANGELOG")

  {
    printf "%s\n\n" "$HEADER"
    echo -e "$CHANGELOG_ENTRY"
    echo "---"
    echo ""
    echo "$BODY"
  } > "${CHANGELOG}.tmp" && mv "${CHANGELOG}.tmp" "$CHANGELOG"

  echo "✅ Index entry prepended → CHANGELOG.md"
  echo ""
  echo "Next steps:"
  echo "  1. Open CHANGELOG.md — fill in the one-line summary for v$VERSION"
  echo "  2. Open $RELEASE_NOTE_FILE — fill in Summary, Migration Notes, Known Issues"
  echo "  3. Remove all <!-- instructional comment --> blocks before publishing"
  echo "  4. Tick off the Deployment Checklist during deployment"
else
  echo "------------------------------------------"
  echo "  To write the release note file and update CHANGELOG.md run:"
  echo "  bash scripts/update-release-notes.sh $VERSION --apply"
  echo "------------------------------------------"
fi