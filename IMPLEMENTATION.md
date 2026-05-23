# IMPLEMENTATION.md

## Scaffold Status Overview

What is **done** (scaffold stage) vs what needs production wiring to satisfy `plan.md` acceptance criteria.

---

## Foundation ✅

| Item | Status | Details |
|---|---|---|
| Maven BOM | ✅ | Root `pom.xml` owns all versions; child poms have no standalone `<version>` |
| Java 17 + Maven 3.9+ | ✅ | `maven-enforcer-plugin` at `validate` |
| Dependency convergence | ✅ | `maven-enforcer-plugin` |
| Google Java Format | ✅ | `spotless-maven-plugin` at `verify` |
| OSGi bnd plugin | ✅ | Parent `pluginManagement`; library modules opt in |
| Package alignment | ✅ | All modules: `com.observability.<module>` |
| Docker Compose backplane | ✅ | Kafka, Redis, Elasticsearch, Kibana, Prometheus, Grafana, OTel Collector |
| `health.sh` | ✅ | Build + package + config + container + port checks |
| `stack.sh` | ✅ | `up` / `down` / `restart` / `status` / `logs <svc>` |
| `run.sh` | ✅ | Auto-detects Spring Boot vs plain Java, runs correct Maven goal |
| `strict.sh` | ✅ | `check` (verify) / `fix` (spotless:apply + verify) |
| `AGENTS.md` | ✅ | AI agent guidance with workflow, conventions, gotchas |
| `README.md` | ✅ | Quick-start, module map, standards, daily loop |

---

## Project 1 — Log Aggregation System · `log-aggregator/`

### ✅ Scaffold Done

| Class | What it does |
|---|---|
| `LogLevel` | Enum: `INFO`, `WARN`, `ERROR` |
| `LogEvent` | Record: `timestampMs`, `service`, `level`, `message` |
| `SlidingWindowErrorRateCalculator` | In-memory deque-backed sliding window (configurable `windowMs`). Evicts stale events on every read. Exposes `totalCount(now)`, `errorCount(now)`, `errorRatePercent(now)`. |
| `LogAggregatorApplication` | Seeds 5 cross-service events across `api-gateway`, `notification`, `rate-limiter`, `metrics` — queries calculator, prints error rate. |

**Sample output:**
```
log-aggregator scaffold is running.
Window total events: 5
Window error events: 2
Window error rate: 40.00%
```

**Key detail — eviction:** every call to `totalCount` / `errorCount` evicts timestamps older than `windowMs` from the front of the deque before counting. This means the window is always accurate to the moment of the call with zero background threads.

### ⬜ Still Needed (`plan.md`)
- [ ] Kafka producer: publish `LogEvent` JSON to topic `log-events`
- [ ] Kafka consumer: batch poll → deserialise → feed `SlidingWindowErrorRateCalculator`
- [ ] Async hand-off: consumer thread submits work to `ExecutorService` (non-blocking poll loop)
- [ ] JVM tuning: `JAVA_OPTS` heap + GC flags in `docker-compose.yml`
- [ ] Docker Compose service entry for `log-aggregator` container

---

## Project 2 — Distributed Rate Limiter · `rate-limiter/`

### ✅ Scaffold Done

| Class | What it does |
|---|---|
| `SlidingWindowRateLimiter` | `ConcurrentHashMap<String, Deque<Long>>` per `userId`. Each bucket is `synchronized` individually — evict expired timestamps → check count → add current time. `allow(userId)` returns boolean. `currentLoad(userId)` returns active request count. |
| `RateLimiterApplication` | 12 rapid requests for `user-A` at 5 req/10 s limit. Prints `ALLOW` / `BLOCK` per request. |

**Key detail — race safety:** `ConcurrentHashMap` ensures bucket-creation atomicity; inner `synchronized(bucket)` block makes evict + count + add atomic per user, so two concurrent threads cannot both read "4 requests" and both be allowed when the limit is 5.

### ⬜ Still Needed (`plan.md`)
- [ ] Redis backend: replace in-memory map with Redis sorted set + Lua script (atomic sliding window across instances)
- [ ] Distributed lock: `SET key workerId NX PX ttl` for cross-pod correctness
- [ ] HTTP endpoint: `POST /rate-limit/{userId}` → `200 OK` or `429 Too Many Requests`
- [ ] Kubernetes manifests: `Deployment` (3 replicas) + `Service` in `k8s/rate-limiter/`

---

## Project 3 — Notification / Event Platform · `notification/`

### ✅ Scaffold Done

| Class | What it does |
|---|---|
| `NotificationEvent` | Record: `id`, `channel` (EMAIL / SMS / PUSH), `payload`, `attempt`. `nextAttempt()` returns new record with `attempt + 1`. |
| `NotificationDispatcher` | Three channel-specific `ExecutorService` pools (2 threads each). `dispatch(event)` submits `sendWithRetry` to the matching pool. `sendWithRetry` loops until `maxAttempts`; routes to in-memory `deadLetterQueue` on exhaustion. Simulated failure: 25% chance (`hash % 4 == 0`). `shutdownGracefully()` drains all pools with 2 s timeout per pool. |
| `NotificationApplication` | Dispatches 6 mixed-channel events, waits 3 s, prints DLQ count. |

**Key detail — per-channel isolation:** EMAIL / SMS / PUSH each own a dedicated pool so a slow email provider can never stall SMS or PUSH delivery. This mirrors the `plan.md` requirement for "custom thread pool per notification type".

### ⬜ Still Needed (`plan.md`)
- [ ] Kafka producer: publish `NotificationEvent` to topic `notification-events`
- [ ] Kafka consumer: Spring Boot `@KafkaListener` per channel, one consumer group per channel type
- [ ] Real exponential backoff: `Thread.sleep(baseMs * 2^attempt)` between retries (currently retries are immediate)
- [ ] Spring Boot wiring: `@SpringBootApplication`, `application.properties`, `log4j2.xml` (copy `metrics/` pattern)
- [ ] Architecture doc: at-least-once delivery guarantee explanation (DLQ + committed Kafka offsets)

---

## Project 4 — File Search Engine · `file-search/`

### ✅ Scaffold Done

| Class | What it does |
|---|---|
| `RankedDocument` | Record: `path`, `score` (term frequency count for the searched term in that file). |
| `InvertedIndex` | BFS with `ArrayDeque` (`addLast` enqueue / `removeFirst` dequeue). Visits every `.java` file under root. Lowercases content, splits on `[^a-z0-9_]+`, accumulates `Map<Path, Integer>` per token. `search(term, limit)` looks up postings map, sorts by score descending, returns top `limit`. `tokenCount()` returns distinct token count. |
| `FileSearchApplication` | Indexes all `.java` files under project `src/main/`, searches `"observability"`, `"window"`, `"import"` — prints ranked results with file paths and scores. |

**Key detail — BFS vs DFS:** BFS (`ArrayDeque` as queue with `addLast`/`removeFirst`) visits shallower directories first — efficient for wide shallow trees. To switch to DFS, change `addLast` → `addFirst` — no other code changes needed.

### ⬜ Still Needed (`plan.md`)
- [ ] `ForkJoinPool` parallel indexing: split directory list into `RecursiveTask<Void>` subtasks, one per subdirectory
- [ ] TF-IDF scoring: weight term frequency by `log(totalDocs / docsContainingTerm)` for true relevance ranking
- [ ] HTTP endpoint: Quarkus `@Path("/search") @GET` returning JSON results
- [ ] Quarkus wiring: `quarkus-log4j2-extension` as runtime dependency in this module's `pom.xml`

---

## Project 5 — Metrics Monitoring System · `metrics/`

### ✅ Scaffold Done

| Class | What it does |
|---|---|
| `CircularDoubleBuffer` | Fixed-capacity ring buffer on `double[]`. `add(value)` writes at `writePos % capacity` and increments `writePos`. `snapshot()` returns values oldest-first: segment `[writePos..capacity)` then `[0..writePos)` — handles the wraparound case correctly. Thread-safe via `synchronized`. |
| `SlidingPercentileCalculator` | `percentile(values, p)` — copies + sorts input, uses linear interpolation between adjacent ranks for non-integer percentile positions. Edge cases: empty array → `0.0`, single value → that value. |
| `MetricsApplication` | Feeds 20 simulated latency samples (20–200 ms Gaussian-shaped), prints p50 / p90 / p99 before Spring Boot context starts. |

**Key detail — `snapshot()` wraparound:** building the result array in two segments (`[writePos..capacity)` + `[0..writePos)`) ensures the caller always receives values in insertion order, regardless of how many times the buffer has wrapped around.

### ⬜ Still Needed (`plan.md`)
- [ ] Prometheus `/metrics` endpoint: add `micrometer-registry-prometheus` + `spring-boot-actuator`
- [ ] `/proc` reader: `ProcFileReader` class reading `/proc/stat` and `/proc/meminfo` for CPU/memory metrics
- [ ] Per-service latency tracking: wire all 7 services to POST response time samples to `metrics/` service
- [ ] Grafana dashboard JSON: pre-built dashboard file with p50 / p90 / p99 panels for all services

---

## Project 6 — API Gateway · `api-gateway/`

### ✅ Scaffold Done

| Class | What it does |
|---|---|
| `BackendInstance` | Record: `id`, `host`, `port`, `activeConnections` (`AtomicInteger`). `incrementConnections()` / `decrementConnections()` for least-connections tracking. |
| `RoundRobinLoadBalancer` | `AtomicInteger` counter. `next()` returns `instances.get(counter.getAndIncrement() % size)`. Thread-safe by atomic increment — no locking needed. |
| `SimpleCircuitBreaker` | Two fields drive state: `failureCount` + `openedAt`. CLOSED when `openedAt == 0`. OPEN when failures hit threshold. Timeout elapsed on the next `allowRequest(now)` call resets both fields atomically — no explicit HALF-OPEN state needed. `onSuccess()` resets. `onFailure(now)` increments and trips if threshold reached. |
| `GatewayRouter` | One `RoundRobinLoadBalancer` + one `SimpleCircuitBreaker` per `backendId`. `route(backendId, now)` checks circuit first, then picks instance. Returns `Optional<BackendInstance>` (empty when circuit is OPEN). |
| `ApiGatewayApplication` | Simulates 10 requests, forces 3 consecutive failures to trip circuit, prints routing decisions + circuit state per request. |

**Circuit breaker state machine:**
```
CLOSED ──(failures >= threshold)──► OPEN
OPEN   ──(timeout elapsed on next allowRequest)──► CLOSED (reset)
```

### ⬜ Still Needed (`plan.md`)
- [ ] Raw HTTP server: `java.net.ServerSocket` request parser or Spring WebFlux `@RestController`
- [ ] Least-connections algorithm: `BackendInstance.activeConnections` — always pick the instance with minimum active count
- [ ] Rate Limiter integration: call `SlidingWindowRateLimiter.allow(userId)` before routing; return `429` if blocked
- [ ] In-memory request cache: `ConcurrentHashMap<String, CachedResponse>` with TTL eviction for idempotent GETs
- [ ] Connection pool: manage upstream HTTP connections (reuse sockets, cap concurrency per backend)

---

## Project 7 — Distributed Job Scheduler · `job-scheduler/`

### ✅ Scaffold Done

| Class | What it does |
|---|---|
| `ScheduledJob` | Record: `id`, `name`, `executeAtMillis`, `priority`. `compareTo` orders by `executeAtMillis` ascending, then `priority` descending (higher value wins at equal time). |
| `DistributedJobScheduler` | `PriorityQueue<ScheduledJob>` ordered by `compareTo`. `pollDueJobs()` drains all jobs where `executeAtMillis <= now`. `heartbeat(workerId)` records last-seen epoch. `staleWorkers(ttlMillis)` returns worker IDs not seen within TTL. |
| `JobSchedulerApplication` | Schedules 4 jobs at different time offsets and priorities, polls due jobs, registers 3 workers with different heartbeat ages, prints stale workers. |

**Key detail — priority ordering:** `compareTo` uses `executeAtMillis` ascending first. For equal execution times, `priority2 - priority1` gives descending order — priority-9 runs before priority-1 at the same scheduled millisecond.

### ⬜ Still Needed (`plan.md`)
- [ ] Redis distributed lock: `SET job:<id> <workerId> NX PX <ttl>` — ensures only one worker executes each job across 5+ pods
- [ ] Job requeue on worker death: detect stale worker → re-add its in-flight jobs back to the priority queue
- [ ] Persistent job store: Redis `ZADD jobs <executeAtMillis> <jobId>` so jobs survive pod restarts
- [ ] Kubernetes: 5-replica `Deployment` showing distributed coordination in `k8s/job-scheduler/`
- [ ] JVM tuning: GC flags for background processing in `docker-compose.yml` `environment.JAVA_OPTS`

---

## 4 OSS Integration Layers (Library Modules)

| Module | Status | Next Step |
|---|---|---|
| `log4j2-otel-appender` | ⬜ Scaffold | `AbstractAppender` implementation → OTel `LogRecordExporter` via OTLP gRPC `:4317` |
| `log4j2-spring-starter` | ⬜ Scaffold | `@AutoConfiguration` that sets `log4j2.xml` as default Spring Boot logging config (replaces Logback) |
| `log4j2-audit-appender` | ⬜ Scaffold | `AbstractAppender` writing to Elasticsearch `audit-logs` index via REST high-level client |
| `quarkus-log4j2-extension` | ⬜ Scaffold | Quarkus extension descriptor + deployment module registering Log4j2 as Quarkus logging backend |

---

## Execution Checklist (matches `plan.md` vacation plan)

- [x] Day 1–3: Repo structure · Docker Compose · all 7 services compilable · Log4j2 convention · scripts · docs
- [ ] Day 4–7: Kafka wiring (Project 1) · Redis backend (Project 2)
- [ ] Day 8–14: Spring Boot service (Project 3) · Quarkus service (Project 4)
- [ ] Day 15–18: `log4j2-otel-appender` live · `traceId` across all services · Grafana dashboard
- [ ] Day 19–22: `log4j2-audit-appender` live · Kibana showing audit logs
- [ ] Day 23–26: HTTP gateway (Project 6) · Redis locks (Project 7)
- [ ] Day 27–30: Polish · architecture diagram · GitHub issues in target company repos

