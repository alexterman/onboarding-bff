#!/usr/bin/env bash
# Verification test for update_sdd.sh: runs it against a throwaway repo copy and fixture zips.
set -euo pipefail
HERE="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
W="$(mktemp -d "${TMPDIR:-/tmp}/sdd_test.XXXXXX")"; trap 'rm -rf "$W"' EXIT
fail() { echo "FAIL: $*" >&2; exit 1; }
R="$W/repo"; mkdir -p "$R/scripts" "$R/docs" "$R/specs" "$R/src/main/kotlin"
cp "$HERE/update_sdd.sh" "$R/scripts/"
echo old > "$R/docs/a.md"; echo old > "$R/specs/s.md"
echo "custom readme" > "$R/README.md"; echo "code" > "$R/src/main/kotlin/A.kt"; echo "plugins{}" > "$R/build.gradle.kts"
snap() { (cd "$R" && cat README.md build.gradle.kts src/main/kotlin/A.kt | shasum); }
BEFORE="$(snap)"

mkz() { rm -rf "$W/z" "$2"; mkdir -p "$W/z/$1/docs" "$W/z/$1/specs"; echo new > "$W/z/$1/docs/a.md"; echo new > "$W/z/$1/specs/s.md"; echo new > "$W/z/$1/README.md"; (cd "$W/z" && zip -qr "$2" "$1"); }

# missing archive
SDD_ZIP="$W/none.zip" "$R/scripts/update_sdd.sh" 2>/dev/null && fail "missing zip accepted"
# wrong root folder
mkz wrong-root "$W/bad.zip"
SDD_ZIP="$W/bad.zip" "$R/scripts/update_sdd.sh" 2>/dev/null && fail "wrong root accepted"
grep -q old "$R/docs/a.md" || fail "docs changed on invalid archive"
# dry run
mkz onboarding-engine-sdd "$W/good.zip"
(cd / && SDD_ZIP="$W/good.zip" "$R/scripts/update_sdd.sh" --dry-run >/dev/null)
grep -q old "$R/docs/a.md" || fail "dry-run modified docs"
# real run from another cwd, twice
(cd / && SDD_ZIP="$W/good.zip" "$R/scripts/update_sdd.sh" >/dev/null)
(cd / && SDD_ZIP="$W/good.zip" "$R/scripts/update_sdd.sh" >/dev/null)
grep -q new "$R/docs/a.md" && grep -q new "$R/specs/s.md" || fail "docs not updated"
[ "$(snap)" = "$BEFORE" ] || fail "code/build/README modified"
# rollback on failure: make verification fail by making specs destination unwritable mid-run
chmod 555 "$R"; mkz onboarding-engine-sdd "$W/good2.zip"
SDD_ZIP="$W/good2.zip" "$R/scripts/update_sdd.sh" >/dev/null 2>&1 && { chmod 755 "$R"; fail "expected failure on read-only repo"; }
chmod 755 "$R"
grep -q new "$R/docs/a.md" && [ -d "$R/specs" ] || fail "docs/specs lost after failed run"
echo "PASS: update_sdd.sh"
