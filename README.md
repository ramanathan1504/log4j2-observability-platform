# Log4j2 Observability Platform

A production-grade distributed system demonstrating end-to-end observability using Apache Log4j2 — built by Apache Log4j2 Committer.

---

## What This Demonstrates

→ **OpenTelemetry-native Log4j2 integration** — automatic trace context injection (`traceId` in every log line)

→ **Spring Boot Log4j2 as first-class citizen** — zero-config switch from Logback via `log4j2-spring-starter`

→ **Elasticsearch audit appender** — complete security audit trail via `log4j2-audit-appender`

→ **Quarkus Log4j2 extension** — GraalVM native compilation support via `quarkus-log4j2-extension`

---

## Architecture

```
Client → API Gateway (Project 6)
           ├─ Rate Limiter    (Project 2 · Redis)
           ├─ Log Aggregator  (Project 1 · Kafka)
           ├─ Notification    (Project 3 · Spring Boot)
           ├─ File Search     (Project 4 · Quarkus)
           ├─ Metrics         (Project 5 · Prometheus)
           └─ Job Scheduler   (Project 7 · Quarkus)

All services → OTel Collector → Elasticsearch (logs/traces) + Prometheus (metrics) → Grafana
```

---

## Stack

| Layer | Technology |
|---|---|
| Services | Spring Boot 3, Quarkus |
| Log framework | Apache Log4j2 |
| Tracing | OpenTelemetry (OTLP gRPC :4317 / HTTP :4318) |
| Event bus | Kafka 3.7 (KRaft, no Zookeeper) |
| Cache / locks | Redis 7.2 |
| Log & trace store | Elasticsearch 8.13 + Kibana |
| Metrics | Prometheus + Grafana |
| Containers | Docker Compose |

---

## Quick Start

### 1. Start the observability backplane

```bash
docker compose up -d
```

| UI | URL |
|---|---|
| Grafana | http://localhost:3000 |
| Kibana | http://localhost:5601 |
| Prometheus | http://localhost:9090 |

### 2. Build all modules

```bash
mvn clean package
```

### 3. Run a single service

```bash
mvn -pl metrics spring-boot:run
```

### 4. Run strict quality gates (recommended before every push)

```bash
mvn verify
```

Or use the helper script:

```bash
./scripts/strict.sh check
```

If Spotless fails, auto-fix formatting and verify again:

```bash
mvn spotless:apply
mvn verify
```

Or run auto-fix + verify in one step:

```bash
./scripts/strict.sh fix
```

### 5. Run a full workspace health check

```bash
./scripts/health.sh
```

---

## Daily Developer Loop

1. Start infra stack:

```bash
./scripts/stack.sh up
```

2. Run or build the module you are editing:

```bash
./scripts/run.sh log-aggregator
```

3. Before commit/push, enforce quality gates:

```bash
./scripts/strict.sh check
```

4. If formatting fails, auto-fix and re-check:

```bash
./scripts/strict.sh fix
```

---

## Module Map

| Module | Type | Description |
|---|---|---|
| `log4j2-otel-appender` | Library | OTel Log4j2 appender (Layer 1) |
| `log4j2-audit-appender` | Library | Elasticsearch audit appender (Layer 3) |
| `log4j2-spring-starter` | Library | Spring Boot auto-config for Log4j2 (Layer 2) |
| `quarkus-log4j2-extension` | Library | Quarkus Log4j2 extension (Layer 4) |
| `log-aggregator` | Service | Kafka log pipeline · sliding-window error rates |
| `rate-limiter` | Service | Redis distributed sliding-window rate limiter |
| `notification` | Service | Async event platform · DLQ · exponential backoff |
| `file-search` | Service | Inverted-index file search engine (Quarkus) |
| `metrics` | Service | Prometheus `/metrics` endpoint · p50/p90/p99 |
| `api-gateway` | Service | HTTP routing · circuit breaker · load balancing |
| `job-scheduler` | Service | Distributed cron · Redis locks (Quarkus) |

---

## Implementation Status

| Project | Module | Scaffold | Production Wiring Needed |
|---|---|---|---|
| P1 · Log Aggregator | `log-aggregator` | ✅ Sliding-window error rate (`SlidingWindowErrorRateCalculator`) | ⬜ Kafka producer/consumer, async hand-off, Docker service |
| P2 · Rate Limiter | `rate-limiter` | ✅ Thread-safe in-memory sliding window (`SlidingWindowRateLimiter`) | ⬜ Redis backend + Lua script, HTTP endpoint, K8s 3-replica |
| P3 · Notification | `notification` | ✅ Per-channel async pools, retry, DLQ (`NotificationDispatcher`) | ⬜ Kafka topics, exponential backoff, Spring Boot wiring |
| P4 · File Search | `file-search` | ✅ BFS indexer + inverted index + ranked search (`InvertedIndex`) | ⬜ ForkJoinPool parallel indexing, TF-IDF scoring, Quarkus HTTP |
| P5 · Metrics | `metrics` | ✅ Circular buffer + p50/p90/p99 (`CircularDoubleBuffer`) | ⬜ Prometheus `/metrics` endpoint, `/proc` reader, Grafana dashboard |
| P6 · API Gateway | `api-gateway` | ✅ Round-robin load balancer + circuit breaker (`GatewayRouter`) | ⬜ HTTP server, least-connections, rate limiter integration |
| P7 · Job Scheduler | `job-scheduler` | ✅ Priority queue + heartbeat + stale workers (`DistributedJobScheduler`) | ⬜ Redis locks, job requeue on worker death, K8s 5-replica |
| Layer 1 | `log4j2-otel-appender` | ⬜ Scaffold only | `AbstractAppender` → OTel `LogRecordExporter` via OTLP gRPC |
| Layer 2 | `log4j2-spring-starter` | ⬜ Scaffold only | `@AutoConfiguration` setting Log4j2 as Spring Boot default |
| Layer 3 | `log4j2-audit-appender` | ⬜ Scaffold only | `AbstractAppender` → Elasticsearch `audit-logs` index |
| Layer 4 | `quarkus-log4j2-extension` | ⬜ Scaffold only | Quarkus extension + deployment module |

See [`IMPLEMENTATION.md`](IMPLEMENTATION.md) for full class-by-class detail, algorithm explanations, and per-project next steps.

---

## Package Convention

All modules use `com.observability.<module>` — e.g. `com.observability.metrics.MetricsApplication`.
Source directories match: `src/main/java/com/observability/<module>/`.

## Log Format

Every log line includes `[traceId=%X{traceId}]` via `log4j2.xml` and `application.properties` pattern.
See `metrics/src/main/resources/log4j2.xml` as the canonical template.

## Developer Standards

- Root `pom.xml` is the single source of truth for shared dependency/plugin versions.
- `maven-enforcer-plugin` (parent) requires Java 17, Maven 3.9+, and dependency convergence.
- `spotless-maven-plugin` (parent) runs at `verify` and fails on formatting drift.
- OSGi bnd support is pre-configured in parent `pluginManagement` (`biz.aQute.bnd:bnd-maven-plugin`); library modules can opt in by declaring the plugin in their module `pom.xml`.

## Scripts Quick Reference

| Script | Command | Purpose |
|---|---|---|
| Strict check | `./scripts/strict.sh check` | Run enforcer + spotless check + verify |
| Strict auto-fix | `./scripts/strict.sh fix` | Format then re-verify |
| Health | `./scripts/health.sh` | Build/package/config/container/port checks |
| Stack control | `./scripts/stack.sh up\|down\|status` | Start/stop/check observability backplane |
| Module run | `./scripts/run.sh <module>` | Run/compile one module quickly |

