# Project Deadlines

One platform — 7 projects — 4 OSS layers — 30 days.

---

## Timeline Overview

| Project | Module | Branch | Days | Status |
|---|---|---|---|---|
| Foundation | repo setup, scripts, BOM, Docker Compose | `main` | Day 1–3 | ✅ Done |
| Project 1 | `log-aggregator` | `feature/project-1-log-aggregator` | Day 4–5 | 🔄 In Progress |
| Project 2 | `rate-limiter` | `feature/project-2-rate-limiter` | Day 6–7 | ⬜ Pending |
| Project 3 | `notification` | `feature/project-3-notification` | Day 8–11 | ⬜ Pending |
| Project 4 | `file-search` | `feature/project-4-file-search` | Day 12–14 | ⬜ Pending |
| Project 5 | `metrics` | `feature/project-5-metrics` | Day 15–16 | ⬜ Pending |
| Layer 1 | `log4j2-otel-appender` | `feature/layer-1-otel-appender` | Day 15–18 | ⬜ Pending |
| Layer 3 | `log4j2-audit-appender` | `feature/layer-3-audit-appender` | Day 19–22 | ⬜ Pending |
| Project 6 | `api-gateway` | `feature/project-6-api-gateway` | Day 23–25 | ⬜ Pending |
| Project 7 | `job-scheduler` | `feature/project-7-job-scheduler` | Day 26–27 | ⬜ Pending |
| Layer 2 | `log4j2-spring-starter` | `feature/layer-2-spring-starter` | Day 28 | ⬜ Pending |
| Layer 4 | `quarkus-log4j2-extension` | `feature/layer-4-quarkus-extension` | Day 29 | ⬜ Pending |
| Polish & publish | README, architecture diagram, GitHub issues | `main` | Day 30 | ⬜ Pending |

---

## Project 1 — Log Aggregation System

**Branch:** `feature/project-1-log-aggregator`
**Deadline:** Day 4–5
**Status:** 🔄 In Progress

### Acceptance Checklist
- [ ] Kafka producer: publishes `LogEvent` JSON to topic `log-events`
- [ ] Kafka consumer: batch poll, deserialise, feed `SlidingWindowErrorRateCalculator`
- [ ] Async hand-off: consumer thread submits to `ExecutorService` — non-blocking poll loop
- [ ] JVM tuning: `JAVA_OPTS` heap + GC flags in `docker-compose.yml`
- [ ] Docker Compose service entry for `log-aggregator` container
- [ ] `strict.sh check` passes on branch before merge

---

## Project 2 — Distributed Rate Limiter

**Branch:** `feature/project-2-rate-limiter`
**Deadline:** Day 6–7
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] Redis sorted set backend + Lua script for atomic sliding window across instances
- [ ] Distributed lock: `SET key workerId NX PX ttl`
- [ ] HTTP endpoint: `POST /rate-limit/{userId}` → `200 OK` or `429 Too Many Requests`
- [ ] Kubernetes manifests: `Deployment` (3 replicas) + `Service` in `k8s/rate-limiter/`
- [ ] `strict.sh check` passes on branch before merge

---

## Project 3 — Notification / Event Platform

**Branch:** `feature/project-3-notification`
**Deadline:** Day 8–11
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] Kafka producer: publish `NotificationEvent` to topic `notification-events`
- [ ] Kafka consumer: Spring Boot `@KafkaListener` per channel (EMAIL / SMS / PUSH)
- [ ] Exponential backoff: `Thread.sleep(baseMs * 2^attempt)` between retries
- [ ] Spring Boot wiring: `@SpringBootApplication`, `application.properties`, `log4j2.xml`
- [ ] Architecture doc: at-least-once delivery guarantee (DLQ + committed Kafka offsets)
- [ ] `strict.sh check` passes on branch before merge

---

## Project 4 — File Search Engine

**Branch:** `feature/project-4-file-search`
**Deadline:** Day 12–14
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] `ForkJoinPool` parallel indexing: `RecursiveTask<Void>` per subdirectory
- [ ] TF-IDF scoring: weight term frequency by `log(totalDocs / docsContainingTerm)`
- [ ] HTTP endpoint: Quarkus `@Path("/search") @GET` returning JSON results
- [ ] Quarkus wiring: `quarkus-log4j2-extension` as runtime dependency
- [ ] `strict.sh check` passes on branch before merge

---

## Project 5 — Metrics Monitoring System

**Branch:** `feature/project-5-metrics`
**Deadline:** Day 15–16
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] Prometheus `/metrics` endpoint: `micrometer-registry-prometheus` + `spring-boot-actuator`
- [ ] `/proc` reader: `ProcFileReader` for Linux CPU / memory metrics
- [ ] Per-service latency: all 7 services report response time samples to `metrics/`
- [ ] Grafana dashboard JSON: pre-built p50 / p90 / p99 panels for all services
- [ ] `strict.sh check` passes on branch before merge

---

## Layer 1 — log4j2-otel-appender

**Branch:** `feature/layer-1-otel-appender`
**Deadline:** Day 15–18
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] `AbstractAppender` implementation sending `LogEvent` to OTel `LogRecordExporter` via OTLP gRPC `:4317`
- [ ] `traceId` injected from OTel context into every log line via `%X{traceId}` MDC
- [ ] Wired into all 7 services via `log4j2.xml` appender reference
- [ ] `strict.sh check` passes on branch before merge

---

## Layer 3 — log4j2-audit-appender

**Branch:** `feature/layer-3-audit-appender`
**Deadline:** Day 19–22
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] `AbstractAppender` writing to Elasticsearch `audit-logs` index via REST client
- [ ] Kibana index pattern `audit-logs-*` showing all events
- [ ] Wired into `api-gateway`, `rate-limiter`, `job-scheduler` (security-sensitive modules)
- [ ] `strict.sh check` passes on branch before merge

---

## Project 6 — API Gateway

**Branch:** `feature/project-6-api-gateway`
**Deadline:** Day 23–25
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] Raw HTTP server: `ServerSocket` request parser or Spring WebFlux `@RestController`
- [ ] Least-connections algorithm: always route to `BackendInstance` with minimum `activeConnections`
- [ ] Rate Limiter integration: call `SlidingWindowRateLimiter.allow(userId)` — return `429` if blocked
- [ ] In-memory request cache: `ConcurrentHashMap<String, CachedResponse>` with TTL eviction
- [ ] `strict.sh check` passes on branch before merge

---

## Project 7 — Distributed Job Scheduler

**Branch:** `feature/project-7-job-scheduler`
**Deadline:** Day 26–27
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] Redis distributed lock: `SET job:<id> <workerId> NX PX <ttl>` — one worker per job across 5+ pods
- [ ] Job requeue on worker death: stale worker detected → in-flight jobs re-added to priority queue
- [ ] Persistent job store: Redis `ZADD jobs <executeAtMillis> <jobId>`
- [ ] Kubernetes: 5-replica `Deployment` in `k8s/job-scheduler/`
- [ ] JVM tuning: GC flags in `docker-compose.yml` `JAVA_OPTS`
- [ ] `strict.sh check` passes on branch before merge

---

## Layer 2 — log4j2-spring-starter

**Branch:** `feature/layer-2-spring-starter`
**Deadline:** Day 28
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] `@AutoConfiguration` setting `log4j2.xml` as default Spring Boot logging config
- [ ] Replaces Logback with zero config by consumer
- [ ] `strict.sh check` passes on branch before merge

---

## Layer 4 — quarkus-log4j2-extension

**Branch:** `feature/layer-4-quarkus-extension`
**Deadline:** Day 29
**Status:** ⬜ Pending

### Acceptance Checklist
- [ ] Quarkus extension descriptor + deployment module registering Log4j2 as Quarkus logging backend
- [ ] Used by `file-search` and `job-scheduler`
- [ ] GraalVM native compilation verified
- [ ] `strict.sh check` passes on branch before merge

---

## Day 30 — Polish & Publish

**Branch:** `main`

### Final Checklist
- [ ] Root `README.md` architecture diagram updated with all live services
- [ ] All feature branches merged to `main` via PR
- [ ] GitHub Issues raised in target company repos (Elastic, Red Hat, Spring, Grafana, Confluent)
- [ ] All Docker services start cleanly with `./scripts/stack.sh up`
- [ ] `./scripts/health.sh` — zero failures end to end

