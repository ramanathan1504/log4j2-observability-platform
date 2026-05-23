# AGENTS.md

## Big picture
- This is a Maven multi-module observability platform rooted at `pom.xml`.
- The repo contains 7 app/service modules plus 4 Log4j2 integration modules.
- `plan.md` describes the intended end-state; current code is mostly scaffolding except `metrics/`.

## Start here (agent workflow)
- Read `README.md` and this file before code changes.
- Run `./scripts/health.sh` once per session to confirm local baseline.
- During development, run `./scripts/strict.sh check` before handing off.
- If formatting fails, run `./scripts/strict.sh fix` and re-run `./scripts/strict.sh check`.
- Use module-local changes first; touch shared infra only when required.

## Repository structure to know
- `config/` holds shared runtime config mounted into containers.
- `docker-compose.yml` is the canonical local stack for Kafka, Redis, Elasticsearch, Kibana, Prometheus, Grafana, and the OpenTelemetry Collector.
- `metrics/` is the only module with real Spring Boot config (`application.properties`, `log4j2.xml`).
- Most other app modules currently have a minimal `main` printing `"<module> module is ready."`.

## Build and run workflow
- Build all modules: `mvn clean package`.
- Run one Spring module: `mvn -pl metrics spring-boot:run`.
- Start/stop infra stack: `./scripts/stack.sh up` / `./scripts/stack.sh down`.
- Check strict gates: `./scripts/strict.sh check`.
- Auto-fix formatting and verify: `./scripts/strict.sh fix`.

## Strict developer gates (mandatory)
- Parent `pom.xml` enforces Java 17, Maven 3.9+, and dependency convergence via `maven-enforcer-plugin` at `validate`.
- Parent `pom.xml` runs `spotless:check` at `verify` using Google Java Format; CI/local verify fails on formatting drift.
- Keep versions centralized in root `pom.xml` (`dependencyManagement` + plugin properties).
- Child poms should not pin shared dependency/plugin versions unless module-specific and justified.
- OSGi readiness is configured via `biz.aQute.bnd:bnd-maven-plugin` in parent `pluginManagement`; reusable library modules can opt-in by declaring that plugin locally.

## Java and packaging conventions
- Java source level is 17 across modules.
- Packages follow `com.observability.<module>` and entry points are named `<ModuleName>Application`.
- Keep shared observability behavior consistent with `metrics/src/main/resources/log4j2.xml`, especially the `[traceId=%X{traceId}]` pattern.

## Observability wiring
- `config/otel-collector-config.yaml` exports traces and logs to Elasticsearch and metrics to Prometheus.
- `config/prometheus.yml` scrapes the OTel collector on `otel-collector:8889`.
- If you add new services, keep their log format trace-aware and update compose/prometheus config when they should participate in the demo stack.

## Current codebase gotchas
- Package `com.observability.<module>` is fully consistent — source files live under `src/main/java/com/observability/<module>/`.
- There are no test sources yet; new tests should mirror the module they cover (e.g. `metrics/src/test/java/com/observability/metrics/`).

## Editing priorities
- Prefer small, module-local changes unless you are touching shared infra.
- Put reusable container/config changes in `config/` or `docker-compose.yml`, not inside individual app modules.
- When adding functionality, keep the module names, package names, and compose service names aligned.
