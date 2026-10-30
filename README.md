# Option 2 — Decompose a Legacy Slice (Spring Boot)

**Question answered:** how do you take one slice out of a monolith — **Trade Capture & Validation** — safely, without a big-bang and without breaking clients?

The hard-coded Appendix A `processTrade()` becomes a standalone service, introduced with **strangler** techniques:

| Technique | Where |
|---|---|
| **Public contract first** | `src/main/resources/static/openapi.yaml` (served at `/openapi.yaml`), RFC 7807 errors, tenant-scoped reads |
| **Logic extracted, ids removed** | `core/TradeProcessor` has no client names; what was `if (clientId == "CLIENT_A")` is a typed `TenantPolicy` in `application.yml` |
| **Strangler routing facade** | `core/TradeEngine` + `core/RoutingService`: per tenant `NEW`, `SHADOW` or `LEGACY`, changeable at runtime (`PUT /admin/v1/routing/{tenant}`) = instant rollback |
| **Shadow / parallel run** | In `SHADOW` the new slice serves; legacy logic runs alongside; every difference is logged and counted (`shadow.compared`, `shadow.mismatch`). CLIENT_A runs in SHADOW by default |
| **Characterization (golden-master) tests** | `ParityTest` proves new == legacy for every client and boundary (exactly 1,000,000 is *not* "above 1M") |
| **Anti-corruption layer** | `core/PostingPort` wraps the legacy stored procedure; posting is deliberately **not** rebuilt in this slice |
| **Events replace side effects** | `trade.posted` after commit → adapters: `file-feed` (legacy file drop), `legacy-fx-queue`, `webhook` |

`core/LegacyTradeProcessor` is a stand-in for the existing platform (reached through an ACL in real life). **Delete it, and the SHADOW/LEGACY modes, when the last tenant is cut over.**

This slice is intentionally simple: per-client variation is plain typed config. Generalizing that into a governed extension platform (rules, custom fields, code plugins) is **Option 1**, a separate project.

## Run (pick one)
| Goal | Command |
|---|---|
| Zero install, Postgres-backed | `docker compose up --build` |
| Single container, in-memory DB | `docker build -t option2 . && docker run -p 8080:8080 option2` |
| Local JDK 21 + Maven 3.9 | `make test && make run` |
| Kubernetes | push the image, set it in `k8s/deployment.yaml`, `make k8s` |
| Any PaaS (Cloud Run / ECS / Container Apps) | deploy the image; honours `PORT`, `DB_URL`, `DB_USER`, `DB_PASSWORD`, `ADMIN_API_KEY` |

Then `./scripts/demo.sh`. Health `/actuator/health/{liveness,readiness}` · metrics `/actuator/prometheus`.

## Cut-over playbook for a tenant (what the routing modes are for)
1. `SHADOW`: serve from the new slice, compare against legacy on live traffic; mismatches must be 0 for an agreed window.
2. `NEW`: legacy no longer called. Keep `LEGACY` one flag-flip away for the rollback window.
3. When all tenants are `NEW`: remove `LegacyTradeProcessor`, the comparator and the routing modes; then retire the stored procedure behind `PostingPort`.

## Not done on purpose (where it breaks at scale)
Tenant auth is a header + admin key (use OIDC/JWT) · routing overrides are in-memory per replica (use a config service/DB with change events) · legacy is simulated in-process (real: an ACL client with timeouts and a circuit breaker) · posting is a stub (real: SimpleJdbcCall to `sp_post_trade`, legacy DB stays system of record; CDC to the new store) · delivery is after-commit in-process (use outbox + broker) · `ddl-auto=update` (use Flyway).

## Live-session twists
New client with the same variations → add a `TenantPolicy` block (config, no code). Client wants something policy cannot express → that is the signal to adopt the Option 1 extension model.
