#!/usr/bin/env bash
# strict.sh - run strict parent quality gates.
# Usage:
#   ./scripts/strict.sh check   # mvn verify
#   ./scripts/strict.sh fix     # mvn spotless:apply && mvn verify
#   ./scripts/strict.sh         # defaults to check

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
MODE="${1:-check}"

usage() {
  cat <<'EOF'
Usage: ./scripts/strict.sh [check|fix|help]

Commands:
  check   Run strict checks only (mvn verify)
  fix     Apply formatting fixes, then verify
  help    Show this help
EOF
}

case "$MODE" in
  check)
    echo "Running strict checks: mvn verify"
    mvn -f "$ROOT/pom.xml" verify
    ;;
  fix)
    echo "Applying format fixes: mvn spotless:apply"
    mvn -f "$ROOT/pom.xml" spotless:apply
    echo "Re-running strict checks: mvn verify"
    mvn -f "$ROOT/pom.xml" verify
    ;;
  help|-h|--help)
    usage
    ;;
  *)
    echo "Unknown command: $MODE"
    usage
    exit 1
    ;;
esac

