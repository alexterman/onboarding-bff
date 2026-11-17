#!/usr/bin/env bash
# Refresh repository-root docs/ and specs/ from the downloaded SDD archive.
#
# Usage: scripts/update_sdd.sh [--dry-run]
#   --dry-run   validate the archive and report what would change; modify nothing.
#
# Environment overrides (mainly for tests):
#   SDD_ZIP   path to the archive (default: ~/Downloads/onboarding-engine-sdd.zip)
#
# Only <repo>/docs and <repo>/specs are replaced. src/, Gradle files, CI and the
# root README.md are never touched. Existing docs/specs are backed up first and
# restored automatically if anything fails.
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
ZIP="${SDD_ZIP:-$HOME/Downloads/onboarding-engine-sdd.zip}"
ROOT_NAME="onboarding-engine-sdd"

DRY_RUN=0
case "${1:-}" in
  "") ;;
  --dry-run) DRY_RUN=1 ;;
  -h|--help) sed -n '2,13p' "${BASH_SOURCE[0]}"; exit 0 ;;
  *) echo "error: unknown argument '$1'" >&2; exit 2 ;;
esac

die() { echo "error: $*" >&2; exit 1; }

command -v unzip >/dev/null 2>&1 || die "'unzip' is required but not installed"
[ -f "$ZIP" ] || die "archive not found: $ZIP"
unzip -tq "$ZIP" >/dev/null 2>&1 || die "archive is corrupt or not a valid zip: $ZIP"

TMP="$(mktemp -d "${TMPDIR:-/tmp}/update_sdd.XXXXXX")"
BACKUP="$TMP/backup"
STAGE="$TMP/extract"
mkdir -p "$BACKUP" "$STAGE"
REPLACING=0
SUCCESS=0
BACKED_UP=()

cleanup() {
  local rc=$?
  if [ "$SUCCESS" -ne 1 ] && [ "$REPLACING" -eq 1 ]; then
    echo "error: update failed; restoring previous docs/ and specs/" >&2
    # Only touch directories that were actually moved to the backup.
    for d in ${BACKED_UP[@]+"${BACKED_UP[@]}"}; do
      rm -rf "${REPO_ROOT:?}/$d" || true
      mv "$BACKUP/$d" "$REPO_ROOT/$d" || echo "error: could not restore $d; backup kept at $BACKUP" >&2
    done
  fi
  [ -d "$BACKUP" ] && [ -n "$(ls -A "$BACKUP" 2>/dev/null)" ] && [ "$SUCCESS" -ne 1 ] || rm -rf "$TMP"
  exit "$rc"
}
trap cleanup EXIT
trap 'exit 130' INT TERM

# Validate structure: exactly one top-level entry, named onboarding-engine-sdd/.
TOP="$(unzip -Z1 "$ZIP" | sed 's#^\./##' | awk -F/ 'NF && $1 != "__MACOSX" && $1 != ".DS_Store" {print $1}' | sort -u)"
[ "$TOP" = "$ROOT_NAME" ] || die "archive must contain exactly one top-level '$ROOT_NAME/' folder (found: $(echo "$TOP" | tr '\n' ' '))"

# Reject path traversal / absolute paths.
if unzip -Z1 "$ZIP" | grep -qE '(^/|(^|/)\.\.(/|$))'; then
  die "archive contains unsafe paths"
fi

unzip -q "$ZIP" -d "$STAGE"
SRC="$STAGE/$ROOT_NAME"
[ -d "$SRC/docs" ]  || die "archive is missing $ROOT_NAME/docs/"
[ -d "$SRC/specs" ] || die "archive is missing $ROOT_NAME/specs/"
[ -n "$(ls -A "$SRC/docs")" ]  || die "archive docs/ is empty"
[ -n "$(ls -A "$SRC/specs")" ] || die "archive specs/ is empty"

if [ "$DRY_RUN" -eq 1 ]; then
  echo "dry-run: archive is valid: $ZIP"
  for d in docs specs; do
    echo "--- $d: differences (current vs archive) ---"
    diff -qr "$REPO_ROOT/$d" "$SRC/$d" || true
  done
  echo "dry-run: no files were modified"
  SUCCESS=1
  exit 0
fi

# Backup, then replace only docs/ and specs/.
REPLACING=1
for d in docs specs; do
  if [ -e "$REPO_ROOT/$d" ]; then mv "$REPO_ROOT/$d" "$BACKUP/$d"; BACKED_UP+=("$d"); fi
done
for d in docs specs; do
  cp -R "$SRC/$d" "$REPO_ROOT/$d"
done

# Verify the result matches the archive.
for d in docs specs; do
  diff -qr "$SRC/$d" "$REPO_ROOT/$d" >/dev/null || die "verification failed for $d/"
done

SUCCESS=1
echo "updated docs/ and specs/ from $ZIP (README.md, src/ and build files untouched)"
