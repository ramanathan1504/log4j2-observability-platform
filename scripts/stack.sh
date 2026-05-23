#!/usr/bin/env bash
# stack.sh — Control the full observability Docker stack
# Usage: ./scripts/stack.sh [up|down|status|restart|logs <service>]

set -euo pipefail
ROOT="$(cd "$(dirname "$0")/.." && pwd)"
COMPOSE="docker compose -f $ROOT/docker-compose.yml"

SERVICES=(kafka redis elasticsearch kibana prometheus grafana otel-collector)

usage() {
  echo "Usage: ./scripts/stack.sh <command>"
  echo ""
  echo "Commands:"
  echo "  up          Start all containers (detached)"
  echo "  down        Stop and remove all containers"
  echo "  restart     down + up"
  echo "  status      Show running state + port of each service"
  echo "  logs <svc>  Tail logs for a specific service"
  echo ""
  echo "Services: ${SERVICES[*]}"
}

case "${1:-help}" in

  up)
    echo "🟢  Starting observability stack..."
    $COMPOSE up -d
    echo ""
    echo "Waiting for services to become healthy (15s)..."
    sleep 15
    echo ""
    echo "  Grafana       → http://localhost:3000"
    echo "  Kibana        → http://localhost:5601"
    echo "  Prometheus    → http://localhost:9090"
    echo "  Elasticsearch → http://localhost:9200"
    echo "  Kafka         → localhost:9092"
    echo "  Redis         → localhost:6379"
    echo "  OTel gRPC     → localhost:4317"
    echo "  OTel HTTP     → localhost:4318"
    ;;

  down)
    echo "🔴  Stopping observability stack..."
    $COMPOSE down
    echo "Done."
    ;;

  restart)
    echo "🔄  Restarting observability stack..."
    $COMPOSE down
    $COMPOSE up -d
    echo "Done."
    ;;

  status)
    echo ""
    echo "── Container status ─────────────────────────────"
    $COMPOSE ps
    echo ""
    echo "── Port checks ──────────────────────────────────"
    PORT_MAP=(
      "kafka:9092"
      "redis:6379"
      "elasticsearch:9200"
      "kibana:5601"
      "prometheus:9090"
      "grafana:3000"
      "otel-collector:4317"
    )
    for item in "${PORT_MAP[@]}"; do
      svc="${item%%:*}"
      port="${item##*:}"
      if nc -z localhost "$port" 2>/dev/null; then
        echo "  ✅  $svc → localhost:$port"
      else
        echo "  ❌  $svc → localhost:$port (not reachable)"
      fi
    done
    ;;

  logs)
    if [[ $# -lt 2 ]]; then
      echo "Usage: ./scripts/stack.sh logs <service>"
      echo "Services: ${SERVICES[*]}"
      exit 1
    fi
    $COMPOSE logs -f "$2"
    ;;

  help|--help|-h)
    usage
    ;;

  *)
    echo "❌  Unknown command: $1"
    usage
    exit 1
    ;;
esac

