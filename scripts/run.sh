#!/usr/bin/env bash
# run.sh — Build & run a single Maven module
# Usage: ./scripts/run.sh <module-name>
# Example: ./scripts/run.sh metrics
#          ./scripts/run.sh log-aggregator

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"

VALID_MODULES=(
  log-aggregator
  rate-limiter
  notification
  file-search
  metrics
  api-gateway
  job-scheduler
  log4j2-otel-appender
  log4j2-audit-appender
  log4j2-spring-starter
  quarkus-log4j2-extension
)

# ── Validate input ────────────────────────────────────────────
if [[ $# -lt 1 ]]; then
  echo "Usage: ./scripts/run.sh <module>"
  echo ""
  echo "Available modules:"
  for m in "${VALID_MODULES[@]}"; do echo "  $m"; done
  exit 1
fi

MODULE=$1

FOUND=false
for m in "${VALID_MODULES[@]}"; do
  [[ "$m" == "$MODULE" ]] && FOUND=true && break
done

if [[ "$FOUND" == false ]]; then
  echo "❌ Unknown module: $MODULE"
  echo "Run ./scripts/run.sh with no args to see valid modules."
  exit 1
fi

if [[ ! -d "$ROOT/$MODULE" ]]; then
  echo "❌ Module directory not found: $ROOT/$MODULE"
  exit 1
fi

# ── Check if module has Spring Boot plugin ────────────────────
HAS_SPRING_BOOT=$(grep -l "spring-boot-maven-plugin" "$ROOT/$MODULE/pom.xml" 2>/dev/null || true)

echo ""
echo "🚀  Starting module: $MODULE"
echo "📁  Path:            $ROOT/$MODULE"
echo ""

if [[ -n "$HAS_SPRING_BOOT" ]]; then
  echo "⚙️   Spring Boot detected — using spring-boot:run"
  echo "──────────────────────────────────────────────────"
  mvn -f "$ROOT/pom.xml" -pl "$MODULE" spring-boot:run
else
  echo "⚙️   No Spring Boot plugin — compiling and running detected *Application main"
  echo "──────────────────────────────────────────────────"
  mvn -f "$ROOT/pom.xml" -pl "$MODULE" compile

  MAIN_FILE=$(find "$ROOT/$MODULE/src/main/java" -name "*Application.java" | head -1)
  if [[ -z "$MAIN_FILE" ]]; then
    echo ""
    echo "ℹ️   No *Application main file found in $MODULE."
    exit 0
  fi

  MAIN_CLASS=$(echo "$MAIN_FILE" | sed 's|.*/src/main/java/||;s|\.java$||;s|/|.|g')
  echo "▶ Running $MAIN_CLASS"
  java -cp "$ROOT/$MODULE/target/classes" "$MAIN_CLASS"
fi

