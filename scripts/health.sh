#!/usr/bin/env bash
# health.sh — Full repo + stack health check
# Usage: ./scripts/health.sh

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
PASS=0; FAIL=0

ok()   { echo "  ✅  $1"; PASS=$((PASS + 1)); }
fail() { echo "  ❌  $1"; FAIL=$((FAIL + 1)); }
hdr()  { echo ""; echo "── $1 ──────────────────────────────"; }

# ── 1. Maven build ──────────────────────────────────────────
hdr "Maven compile (all modules)"
if mvn -f "$ROOT/pom.xml" clean compile -q 2>&1; then
  ok "mvn clean compile SUCCESS"
else
  fail "mvn clean compile FAILED"
fi

# ── 2. Java source integrity ─────────────────────────────────
hdr "Java files & package alignment"
JAVA_COUNT=$(find "$ROOT" -name "*.java" | wc -l | tr -d ' ')
STALE=$( (grep -r "com\.ramanathan" --include="*.java" "$ROOT" 2>/dev/null || true) | wc -l | tr -d ' ')
[[ "$JAVA_COUNT" -eq 11 ]] && ok "11 Java source files found" || fail "Expected 11 Java files, found $JAVA_COUNT"
[[ "$STALE" -eq 0 ]]       && ok "No stale ramanathan package refs" || fail "$STALE stale ramanathan refs remain"

# Package/dir alignment
BAD=0
while IFS= read -r f; do
  declared=$(grep "^package" "$f" | awk '{print $2}' | tr -d ';')
  expected=$(echo "$f" | sed 's|.*/src/main/java/||;s|/[^/]*\.java||;s|/|.|g')
  if [[ "$declared" != "$expected" ]]; then
    echo "    MISMATCH: $f"
    BAD=$((BAD + 1))
  fi
done < <(find "$ROOT" -name "*.java")
[[ "$BAD" -eq 0 ]] && ok "All package declarations match directory paths" || fail "$BAD package/dir mismatches"

# ── 3. config files ──────────────────────────────────────────
hdr "Config files"
[[ -f "$ROOT/config/otel-collector-config.yaml" ]] && ok "otel-collector-config.yaml" || fail "otel-collector-config.yaml missing"
[[ -f "$ROOT/config/prometheus.yml" ]]              && ok "prometheus.yml"              || fail "prometheus.yml missing"
[[ -f "$ROOT/metrics/src/main/resources/log4j2.xml" ]]           && ok "metrics/log4j2.xml"           || fail "metrics/log4j2.xml missing"
[[ -f "$ROOT/metrics/src/main/resources/application.properties" ]] && ok "metrics/application.properties" || fail "metrics/application.properties missing"

# ── 4. Docker containers ──────────────────────────────────────
hdr "Docker containers (docker compose ps)"
REQUIRED=(kafka redis elasticsearch kibana prometheus grafana otel-collector)
for svc in "${REQUIRED[@]}"; do
  STATUS=$(docker compose -f "$ROOT/docker-compose.yml" ps --status running --quiet "$svc" 2>/dev/null)
  if [[ -n "$STATUS" ]]; then
    ok "Container running: $svc"
  else
    fail "Container NOT running: $svc  (run: ./scripts/stack.sh up)"
  fi
done

# ── 5. Port availability ──────────────────────────────────────
hdr "Key port checks"
PORTS=(
  "Kafka:9092"
  "Redis:6379"
  "Elasticsearch:9200"
  "Kibana:5601"
  "Prometheus:9090"
  "Grafana:3000"
  "OTel-gRPC:4317"
  "OTel-HTTP:4318"
)
for item in "${PORTS[@]}"; do
  name="${item%%:*}"
  port="${item##*:}"
  if nc -z localhost "$port" 2>/dev/null; then
    ok "Port $port open ($name)"
  else
    fail "Port $port closed ($name)"
  fi
done

# ── Summary ───────────────────────────────────────────────────
echo ""
echo "══════════════════════════════════════════"
echo "  PASSED: $PASS   FAILED: $FAIL"
[[ "$FAIL" -eq 0 ]] && echo "  🟢  All checks passed — ready to code!" \
                     || echo "  🔴  Fix the failures above before starting."
echo "══════════════════════════════════════════"

