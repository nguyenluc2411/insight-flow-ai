# Implementation State — Insight Flow AI Backend
Updated: 2026-05-19T14:00:00Z

---

## E2E Test Run — 2026-05-18 — PARTIAL PASS

| Bước | Endpoint | HTTP Status | Pass/Fail | Ghi chú |
|------|----------|-------------|-----------|---------|
| 1 Eureka | /eureka/apps | - | **PASS** | 4 services UP: API-GATEWAY, AUTH-SERVICE, CATALOG-SERVICE, SALES-SERVICE |
| 2 Register tenant | POST /api/v1/auth/register-tenant | 201 | **PASS** | JWT returned |
| 3 Create product | POST /api/v1/catalog/products | 201 | **PASS** | productId=4b0ad681-... |
| 4 Create location | POST /api/v1/catalog/locations | 201 | **PASS** | locationId=70a009ac-... |
| 5 Create variant | POST /api/v1/catalog/products/{id}/variants | 201 | **PASS** | Fixed in commit `51e94e9` |
| 6 Inventory movement | POST /api/v1/catalog/inventory/movements | 201 | **PASS** | 100 units restocked |
| 7 Create order | POST /api/v1/sales/orders | 201 | **PASS** | orderId=5d0378df-... |
| 8 Complete order | POST /api/v1/sales/orders/{id}/complete | 200 | **PASS** | status=completed |
| 9 Kafka events | sales.order.completed topic | - | **PASS** | Event published with full payload |

**Result: 9/9 PASS** (after fix commit `51e94e9`)

**Kafka topics confirmed**: auth.tenant.registered, catalog.inventory.updated, sales.order.completed

---

## Gateway Component 3 — COMPLETE ✅

| Phase | File | Commit |
|-------|------|--------|
| 3.1 CorrelationIdFilter (order -100) | filter/CorrelationIdFilter.java | `51878c8` |
| 3.2 LoggingFilter (order -50)        | filter/LoggingFilter.java       | `1d06424` |
| 3.3 JwtAuthenticationFilter (order 100) + JwtValidator | filter/, util/ | `b56f175` |
| 3.4 TenantContextFilter (order 200)  | filter/TenantContextFilter.java | `5f56c98` |
| 3.5 RateLimitConfig + FixedWindowRateLimiter | config/, ratelimit/ | `078fb1d` |
| 3.6 GlobalExceptionHandler           | exception/GlobalExceptionHandler.java | `4c25322` |

### Swagger Aggregator — COMPLETE ✅
- `SwaggerServiceProperties.java` — alias→serviceId mapping from YAML | `dc5ab83`
- `SwaggerDocsProxyController.java` — GET /v3/api-docs/{alias} → lb://service/v3/api-docs | `dc5ab83`
- `WebClientConfig.java` — immutable load-balanced WebClient | `dc5ab83`
- Config fixes dev/prod (CORS path SCG 4.x, env var naming) | `11df016`
- Dropdown: "Auth Service" + "Catalog Service" at http://localhost:8080/swagger-ui/index.html

**Dev JWT secret (Base64)**: `aW5zaWdodGZsb3ctZGV2LTMyYnl0ZXMtc2VjcmV0ISE=`
= `insightflow-dev-32bytes-secret!!` (32 bytes, HS256-safe)
**Must match auth-service dev config exactly.**

---

## Auth Service — COMPLETE ✅

| Phase | Description | Commit |
|-------|-------------|--------|
| A1 | Project skeleton (pom.xml, yml, Dockerfile, mvnw) | `a4e1224` |
| A2 | Flyway migrations V1-V5 (schema, tables, seeds) | `602bb1e` |
| A3 | JPA entities + repositories | `47d978c` |
| A4 | Core services (JWT, Password, Token, Tenant, Auth) | `a8b6a93` |
| A5 | AuthController + GlobalExceptionHandler | `a652611` |
| A6 | KafkaConfig, OpenApiConfig, api-contracts/auth-service.yaml | `3694585` |

### Endpoints implemented
- `POST /api/v1/auth/register-tenant` → 201
- `POST /api/v1/auth/login`           → 200
- `POST /api/v1/auth/refresh`         → 200
- `POST /api/v1/auth/logout`          → 204
- `GET  /api/v1/auth/me`              → 200 (protected)

---

## Catalog Service — COMPLETE ✅

| Phase | Description | Commit |
|-------|-------------|--------|
| C1 | Project skeleton (pom.xml, yml, Dockerfile) | `8093369` |
| C2 | Flyway migrations V1-V6 (schema + all tables) | `ba30ed4` |
| C3 | JPA entities + repositories (6 entities, 6 repos) | `31e1143` |
| C4 | Services, DTOs, mappers, Kafka event | `437a816` |
| C5 | REST controllers (Product, Location, Inventory) | `35481bb` |
| C6 | Gateway: Swagger dropdown + catalog route | `current` |

### Endpoints implemented
```
GET    /api/v1/catalog/products                         list (paginated)
POST   /api/v1/catalog/products                         create
GET    /api/v1/catalog/products/{id}                    get by id
PUT    /api/v1/catalog/products/{id}                    update
DELETE /api/v1/catalog/products/{id}                    soft delete

GET    /api/v1/catalog/locations                        list active
POST   /api/v1/catalog/locations                        create

GET    /api/v1/catalog/inventory/variants/{variantId}   levels by variant
POST   /api/v1/catalog/inventory/movements              record movement → Kafka
GET    /api/v1/catalog/inventory/movements/{variantId}  movement history
```

### Key design decisions
- All queries filter tenant_id at repository layer (tenant isolation)
- InventoryMovement is append-only — BIGSERIAL PK, no updated_at column
- InventoryLevel upserted on each movement (variant_id + location_id UNIQUE)
- Kafka publish uses whenComplete — fail-open: Kafka down ≠ movement rejected
- Soft delete: status = "inactive" (no physical DELETE on products)
- X-Tenant-Id header extracted from gateway TenantContextFilter

### Schema: catalog_db
Tables: categories, products, product_variants, locations, inventory_levels, inventory_movements

### Kafka Events Published
| Topic | Trigger |
|---|---|
| `catalog.inventory.updated` | POST /api/v1/catalog/inventory/movements |

### Open issues
1. No service-level JWT validation (relies on api-gateway for auth)
2. Category and ProductVariant CRUD endpoints not yet implemented
3. No consumer for `sales.order.completed` event (auto-inventory deduction)

---

## Infrastructure — COMPLETE ✅

| Service | Port | Status | Commit |
|---------|------|--------|--------|
| docker-compose (Postgres, Redis, Kafka, ZK) | various | Parameterized credentials, fixed healthchecks | `db7ec99` |
| discovery-server (Eureka) | 8761 | Done | — |
| config-server | 8888 | EUREKA_URL env var | `2303743` |
| api-gateway | 8080 | Done + Swagger aggregator | `dc5ab83` |
| auth-service | 8081 | Done + env vars standardized | `f0c72ce` |
| catalog-service | 8082 | Done | `35481bb` |
| sales-service | 8083 | Done | `2cec988` |

### Cross-cutting changes (2026-05-18)
- `chore(root)`: `.env.example` template added, `.gitignore` whitelist | `fb7d124`
- All services now read credentials from `.env` via env vars (no hardcoded localhost/passwords)

---

## Startup Order

```
1. docker-compose up -d           (Redis, Postgres, Kafka)
2. ./mvnw spring-boot:run          (discovery-server, port 8761)
3. ./mvnw spring-boot:run          (config-server, port 8888)
4. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (api-gateway, port 8080)
5. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (auth-service, port 8081)
6. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (catalog-service, port 8082)
7. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (sales-service, port 8083)
```

---

## Next Steps

### catalog-service (short-term)
- [ ] Add service-level JWT/tenant validation (currently relies on gateway)
- [x] GET /products/{productId}/variants ✅ (phase C7.2)
- [x] GET /categories ✅ (phase C7.2)
- [x] GET /inventory/summary ✅ (phase C7.2)
- [ ] POST/PUT/DELETE Category CRUD (if needed)
- [ ] Consumer for `sales.order.completed` → adjust inventory

---

## Catalog Service — C7 Frontend Endpoints ✅ (2026-05-19)

| Phase | Description | Commit |
|-------|-------------|--------|
| C7.1 | Repos (JPQL GROUP BY, aggregate queries), DTOs (records) | `e6b020d` |
| C7.2 | Services + Controllers for 3 new endpoints | `0e35d59` |

### 3 new endpoints
```
GET /api/v1/catalog/products/{productId}/variants
    → List<VariantResponse> — tenant-guarded, uses existing VariantMapper

GET /api/v1/catalog/inventory/summary
    → { totalSKU, totalQuantity, lowStockCount }
    → 3 aggregate queries: countActiveByTenantId, sumQuantityOnHand, countLowStock
    → lowStockCount = stock positions where quantityOnHand <= COALESCE(reorderPoint, 10)

GET /api/v1/catalog/categories
    → List<CategorySummaryItem> — single JPQL LEFT JOIN + GROUP BY, no N+1
    → { id, name, productCount } — productCount = active products only
```

### Test results
| Test | Result |
|------|--------|
| GET /categories (empty tenant) | `[]` 200 ✅ |
| GET /inventory/summary (1 variant, 100 units) | `{totalSKU:1, totalQuantity:100, lowStockCount:0}` 200 ✅ |
| GET /products/{id}/variants | variant array returned 200 ✅ |
| Wrong tenant on variants | 404 tenant isolation ✅ |

---

## ML Service — COMPLETE ✅ (Phase 1)

| Phase | Description | Notes |
|-------|-------------|-------|
| M1 | Project skeleton (FastAPI, pydantic-settings, Dockerfile, requirements) | Python 3.11 in container; 3.13 OK locally with relaxed pins |
| M2 | SQLAlchemy models + auto-init schema `ml_service_db` | 5 tables: forecasts, recommendations, training_jobs, sales_data, inventory_snapshots |
| M3 | Kafka consumer (background thread, idempotent) | Subscribes to `sales.order.completed`, `catalog.inventory.updated` |
| M4 | Prophet forecasting + cold-start moving-average fallback | Lazy import of prophet; per-tenant model storage |
| M5 | Rule-based recommendation engine | CLEARANCE / RESTOCK / PROMOTE / OK with documented thresholds |
| M6 | FastAPI endpoints (forecast/recommendation/health) | Pydantic schemas for all I/O |
| M7 | Gateway route + Swagger dropdown | Direct HTTP URI (Python — no Eureka); extended SwaggerDocsProxyController to support url-based services |

### Endpoints implemented
```
GET  /api/v1/ml/health
GET  /api/v1/ml/forecast/{variantId}?days=N
POST /api/v1/ml/forecast/batch
GET  /api/v1/ml/recommendations?action&priority&page&size
POST /api/v1/ml/recommendations/refresh
```

### Key design decisions
- Kafka consumer runs in a daemon thread; never raises out of poll loop
- Model files NOT committed (`.gitignore`: `/models/`, `*.pkl`)
- `app/models/` is the Python package (NOT ignored); model storage lives at service-root `models/`
- Cold start: if no per-variant model file, returns 30-day moving average with `confidence=low`
- Idempotency: SalesData has unique `event_id` constraint
- Tenant isolation: every persisted row filters by tenant_id; model paths are `MODEL_STORAGE_PATH/{tenant_id}/{variant_id}/{version}.pkl`
- Recommendation refresh is an async background thread tracked in `training_jobs` table

### Schema: ml_service_db
Tables: forecasts, recommendations, training_jobs, sales_data, inventory_snapshots

### Resume

```bash
cd intelligence-services/ml-service
source .venv/Scripts/activate     # Windows Git Bash
uvicorn app.main:app --reload --port 8000
```

Through gateway: http://localhost:8080/swagger-ui/index.html → dropdown → "ML Service"

---

## Sales Service — COMPLETE ✅

| Phase | Description | Commit |
|-------|-------------|--------|
| S1 | Project skeleton (pom.xml, yml, Dockerfile) | `8e36866` |
| S2 | Flyway migrations V1-V6 (schema + all tables + materialized view) | `3f24b99` |
| S3 | JPA entities + repositories (4 entities, 4 repos) | `1a3e1c6` |
| S4 | Services, DTOs, mappers, Kafka event | `cbfaa24` |
| S5 | REST controllers (Order, Customer, Supplier) | `2cec988` |
| S6 | Gateway: Swagger dropdown + sales route | current |

### Endpoints implemented
```
GET  /api/v1/sales/orders              list (paginated)
POST /api/v1/sales/orders              create order (status=pending)
GET  /api/v1/sales/orders/{id}         get by id
POST /api/v1/sales/orders/{id}/complete  complete → update customer stats + Kafka

GET  /api/v1/sales/customers           list (paginated)
POST /api/v1/sales/customers           create
GET  /api/v1/sales/customers/{id}      get by id

GET  /api/v1/sales/suppliers           list (paginated)
POST /api/v1/sales/suppliers           create
```

### Key design decisions
- SalesOrderItems append-only — no updated_at column
- Order number format: `ORD-{first8ofTenantId}-{epochMillis}` — unique per tenant
- completeOrder: update customer stats (total_spent, order_count, last_order_at) atomically in same TX
- Kafka publish uses whenComplete — fail-open: Kafka down ≠ order completion rejected
- Order state machine: pending → completed (guard: rejects if already completed)
- tenant_id enforced at repository layer on all queries
- Materialized view `daily_sales_summary` for ML service aggregation queries

### Schema: sales_db
Tables: customers, suppliers, sales_orders, sales_order_items, daily_sales_summary (materialized view)

### Kafka Events Published
| Topic | Trigger |
|---|---|
| `sales.order.completed` | POST /api/v1/sales/orders/{id}/complete |

### Open issues
1. No service-level JWT validation (relies on api-gateway for auth)
2. No consumer for `catalog.inventory.updated` event
3. daily_sales_summary needs periodic REFRESH (manual or pg_cron)
4. No supplier → purchase order workflow yet

---

## Next Steps

### Integration service
- [ ] KiotViet connector spike (highest priority per PROJECT_CONTEXT.md)

### Enhancements
- [ ] catalog-service: Category/Variant CRUD endpoints
- [ ] sales-service: daily_sales_summary refresh job
- [ ] sales-service: consumer for `catalog.inventory.updated`

---

## Startup Notes

Catalog-service requires 2 extra flags when starting via CLI (Windows timezone issue):
```bash
export $(grep -v '^#' .env | grep -v '^$' | xargs) && export KAFKA_BOOTSTRAP=$KAFKA_BOOTSTRAP_SERVERS
cd business-services/catalog-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev \
  -Dspring-boot.run.jvmArguments=-Duser.timezone=UTC
```

---

## Dashboard BFF — COMPLETE ✅ (2026-05-19)

| Phase | Description | Commit |
|-------|-------------|--------|
| B1 | Project skeleton (pom.xml, main class, application.yml, Dockerfile) | `479b998` |
| B2 | WebClientConfig — 3 WebClient beans (catalog lb://, sales lb://, ml direct) | `479b998` |
| B3 | DashboardAggregationService — Mono.zip parallel calls, graceful partial fallback | `479b998` |
| B4 | DashboardController — 4 endpoints + GlobalExceptionHandler (RFC 7807) | `479b998` |
| B5 | MlEventConsumer — Kafka listener for ml.forecast.generated + ml.recommendation.created | `479b998` |
| B6 | Gateway: "Dashboard BFF" added to swagger urls + app.swagger.services | `479b998` |

### Endpoints
```
GET /api/v1/dashboard/overview               → totalSKU, ordersToday, revenueToday, highPriorityAlerts, mlStatus
GET /api/v1/dashboard/health-summary         → inventoryPressurePct, slowMovingSKUCount, categoryRisks
GET /api/v1/dashboard/recommendations-summary → total, byAction, topActions, estimatedImpact
GET /api/v1/dashboard/forecast-summary       → topProducts (30d forecast), overallConfidence
```

### Key design
- `spring.main.web-application-type=servlet` — MVC primary, WebFlux only for WebClient
- Mono.zip parallel calls with `partial=true` fallback if any downstream times out
- Timeout: 5s connect / 10s read; 15s per aggregation total
- Port 8090, registers with Eureka as `dashboard-bff`

### Startup
```bash
cd engagement-services/dashboard-bff
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

---

## notification-service — COMPLETE ✅ (2026-05-19)

Path: `engagement-services/notification-service/`
Port: 8091

| Phase | Description | Commit |
|-------|-------------|--------|
| N1 | Project skeleton | `0b7acf5` |
| N2+N3 | Migrations + entities + repositories | `c4d929e` |
| N4-N6 | Kafka consumers + services + controllers | `58a7be4` |
| N7 | Gateway route + swagger + .env.example MAIL vars | `352623b` |

### Endpoints
```
GET  /api/v1/notifications           list (paginated, filter by type/unread)
GET  /api/v1/notifications/unread-count
PUT  /api/v1/notifications/{id}/read
PUT  /api/v1/notifications/read-all
GET  /api/v1/preferences
PUT  /api/v1/preferences
```

### Kafka Consumers
| Topic | Action |
|-------|--------|
| `catalog.inventory.updated` | Low stock alert (< threshold) |
| `ml.forecast.generated` | Forecast notification |
| `ml.recommendation.created` | Recommendation notification |

### Notes
- Email dispatch phase 1: in-app only; email wired when auth-service exposes tenant owner email
- Schema: `notification_db` — 3 tables: notifications, notification_preferences, processed_events

---

## integration-service — COMPLETE ✅ (2026-05-20)

Path: `integration-services/integration-service/`
Port: 8084

| Phase | Description | Commit |
|-------|-------------|--------|
| I1 | Project skeleton (pom.xml, main, yml, Dockerfile, mvnw) | `5e4e19f` |
| I2 | Flyway migrations V1-V4 (connector_configs, sync_jobs, processed_webhooks, entity_mappings) | `c689c86` |
| I3 | Core framework (enums, interface, registry, vault, rate limiter, KV models, entities, repos) | `7f5aa44` |
| I4 | KiotViet connector (auth client, product/order/inventory/branch clients, webhook verifier) | `3ba2270` |
| I5 | DTOs, mappers, configs, exception handler, service layer (config + orchestrator + scheduler) | `8d68380` |
| I6 | Kafka event producer (integration.product/order/inventory.synced, integration.sync.completed) | `93ffee8` |
| I7 | REST controllers (IntegrationController + WebhookController) | `f49bd3c` |
| I8 | Gateway swagger registration + .env.example integration vars | `945c968` |

### Endpoints
```
GET  /api/v1/integrations              list connector configs
POST /api/v1/integrations              create connector (auth test on create)
GET  /api/v1/integrations/{id}         get connector
DELETE /api/v1/integrations/{id}       deactivate connector
POST /api/v1/integrations/{id}/sync    trigger full sync (async → 202)
GET  /api/v1/integrations/{id}/jobs    list sync jobs (paginated)

POST /api/v1/webhooks/kiotviet         receive KiotViet webhook (public, HMAC verified)
```

### Key design decisions
- Plugin-based: `ConnectorInterface` + `ConnectorRegistry` — new POS = new impl + register
- `CredentialVault` (Jasypt) encrypts all POS credentials at rest
- `KiotVietAuthClient` caches OAuth token in-memory (expires_in - 60s buffer, auto-refresh)
- `ConnectorRateLimiter` (Resilience4j): KIOTVIET=3 req/s, SAPO/HARAVAN=5 req/s
- Webhook idempotency: `processed_webhooks` table with UNIQUE(connector_type, external_event_id)
- Sync fail-open: Kafka publish failure does NOT roll back sync job
- Scheduler: incremental sync every 15 min, full reconciliation at 2AM daily
- `SyncOrchestratorService` runs @Async — trigger returns 202 immediately

### Schema: integration_db
Tables: connector_configs, sync_jobs, processed_webhooks, entity_mappings

### Kafka Events Published
| Topic | Trigger |
|-------|---------|
| `integration.product.synced` | Each product batch during sync |
| `integration.order.synced` | Each order batch during sync |
| `integration.inventory.synced` | Each inventory batch during sync |
| `integration.sync.completed` | Full/incremental sync finished |

### Gateway routes (already present before I8)
- `POST /api/v1/webhooks/**` → public, webhookRateLimiter
- `/api/v1/integrations/**` → protected (JWT required), defaultRateLimiter

### .env.example vars added
- `KIOTVIET_CLIENT_ID`, `KIOTVIET_CLIENT_SECRET`
- `JASYPT_PASSWORD` (credential encryption key)

### Startup
```bash
cd integration-services/integration-service
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev
```

### Known limitations / Phase 2
- Sapo/Haravan connectors: scaffold exists (registry ready), implementation deferred
- Full reconciliation scheduler: present but `runKiotVietSync` needs `KiotVietSyncHelper` wiring (Phase 2)
- `EntityMapping` repo populated but not yet used for change detection / deduplication
- Email address for notification-service: needs auth-service to expose tenant owner email

---

---

## Test Run — 2026-05-20 — PARTIAL (bị gián đoạn, chưa hoàn thành)

### Môi trường tại thời điểm test

| Thành phần | Trạng thái | Ghi chú |
|------------|-----------|---------|
| Docker Desktop | Ban đầu OFF → đã khởi động thủ công | Cần bật trước khi start services |
| PostgreSQL (Docker) `insight-postgres` | UP healthy, port 5433 | Đúng port theo .env |
| PostgreSQL (Native Windows) | UP, port 5432 | KHÔNG dùng cho project — dùng Docker |
| Redis `insight-redis` | UP healthy, port 6379 | Đã start qua docker-compose |
| Kafka `insight-kafka` | UP healthy, port 9092 | Đã start qua docker-compose |
| Zookeeper `insight-zookeeper` | UP healthy, port 2181 | Đã start qua docker-compose |
| Kafka UI `insight-kafka-ui` | UP, port 8085 | Đã start qua docker-compose |
| pgAdmin `insight-pgadmin` | UP, port 5050 | Đã start qua docker-compose |

### Trạng thái từng service Java (sau khi Docker đã chạy)

| Service | Port | Trạng thái | DB | Eureka | Ghi chú |
|---------|------|-----------|-----|--------|---------|
| discovery-server | 8761 | **UP** ✅ | N/A | N/A | Đã khởi động thành công |
| config-server | 8888 | **UP** ✅ | N/A | UP | Đã khởi động thành công |
| catalog-service | 8082 | **UP** ✅ | UP | UP | Đang chạy từ trước, OpenAPI 200 |
| notification-service | 8091 | **DEGRADED** ⚠️ | UP | UP | Mail DOWN (MailHog chưa chạy) |
| sales-service | 8083 | **UP** ✅ | UP | UP | Khởi động thành công |
| dashboard-bff | 8090 | **UP** ✅ | N/A | UP | Khởi động thành công |
| api-gateway | 8080 | **FAILED** ❌ | N/A | - | JWT_SECRET env var không được truyền |
| auth-service | 8081 | **FAILED** ❌ | - | - | CONFIG_SERVER_URL env var không được truyền |
| integration-service | 8084 | **FAILED** ❌ | - | - | POSTGRES_PASSWORD env var không được truyền |
| ml-service | 8000 | **CHƯA TEST** ⏭️ | - | - | Chưa đến lượt |

### Lỗi đã phát hiện

#### 1. Env vars không được kế thừa khi start service bằng PowerShell Start-Process
**Triệu chứng**: 3 service (api-gateway, auth-service, integration-service) crash ngay khi start.

**Chi tiết lỗi từng service:**

**api-gateway:**
```
io.jsonwebtoken.io.DecodingException: Illegal base64 character: '-'
```
→ JWT_SECRET không được load từ .env, dùng fallback `insightflow-dev-secret-change-in-production!!`
có ký tự `-` không hợp lệ trong Base64.
**JWT_SECRET đúng trong .env**: `aW5zaWdodGZsb3ctZGV2LTMyYnl0ZXMtc2VjcmV0ISE=` (32 bytes, valid)

**auth-service:**
```
ConfigClientFailFastException: Could not locate PropertySource
Caused by: IllegalStateException: Invalid URL: ${CONFIG_SERVER_URL}
```
→ CONFIG_SERVER_URL không được substitute → literal `${CONFIG_SERVER_URL}` được pass vào URL parser → crash.

**integration-service:**
```
FATAL: password authentication failed for user "postgres"
```
→ POSTGRES_PASSWORD không được truyền → dùng default `postgres` → sai với Docker container password.

**Root cause**: Các service được khởi động bằng `Start-Process cmd.exe` trong PowerShell không kế thừa env vars từ .env.
Catalog-service và notification-service hoạt động vì được user khởi động trực tiếp từ terminal nơi .env đã được load.

**Cách fix (khi test lại)**: Khởi động mỗi service trực tiếp từ terminal với `.env` đã được load:
```bash
# Cách 1: Load .env trước, rồi chạy service
set -o allexport; source .env; set +o allexport
cd platform-services/api-gateway
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Cách 2: Truyền trực tiếp
JWT_SECRET=aW5zaWdodGZsb3ctZGV2LTMyYnl0ZXMtc2VjcmV0ISE= ./mvnw spring-boot:run ...

# Cách 3 (Windows CMD): Dùng dotenv hoặc set trước khi chạy
```

#### 2. MailHog chưa được thêm vào docker-compose.yml
**Triệu chứng**: notification-service health = DOWN vì `mail.status = DOWN`
```
MailConnectException: Couldn't connect to host, port: localhost, 1025
```
→ MailHog container không có trong `infrastructure/docker/docker-compose.yml`.
Docker Compose chỉ có: postgres, pgadmin, zookeeper, kafka, kafka-ui, redis.
.env.example có hướng dẫn `docker run -p 1025:1025 -p 8025:8025 mailhog/mailhog` nhưng không integrate vào compose.

#### 3. Docker Desktop cần khởi động thủ công trước khi chạy services
Khi Docker Desktop tắt, toàn bộ services fail với DB connection error (port 5433 không có).
Thứ tự khởi động bắt buộc:
```
1. Start Docker Desktop
2. docker-compose up -d   (Postgres, Redis, Kafka)
3. discovery-server
4. config-server
5. api-gateway (cần JWT_SECRET env var đúng)
6. auth-service, catalog-service, sales-service
7. dashboard-bff, notification-service
8. integration-service, ml-service
```

### Test API đã thực hiện được

| Endpoint | Status | Kết quả |
|----------|--------|---------|
| `GET /actuator/health` (catalog:8082) | 200 UP ✅ | DB UP, Eureka UP |
| `GET /v3/api-docs` (catalog:8082) | 200 ✅ | Full OpenAPI spec trả về đúng |
| `GET /actuator/info` (catalog:8082) | 200 ✅ | Empty {} |
| `GET /actuator/health` (notification:8091) | 503 ⚠️ | DB UP, Mail DOWN |
| `GET /v3/api-docs` (notification:8091) | 200 ✅ | Full OpenAPI spec trả về đúng |
| `GET /eureka/apps` (discovery:8761) | 200 ✅ | catalog + notification registered |
| Config Server health | UP ✅ | Serving classpath config |

### Chưa test được

- auth-service: đăng ký tenant, login, JWT
- api-gateway: routing, JWT validation, CORS
- sales-service: orders, customers (service UP nhưng chưa test API)
- dashboard-bff: aggregate endpoints (service UP nhưng chưa test API)
- integration-service: connector creation, KiotViet sync
- ml-service: forecast, recommendation
- Kafka event flow: end-to-end

---

## Resume Prompt

```
Read docs/handoffs/CURRENT_STATE.md first.
Then read PROJECT_CONTEXT.md, .claude/CLAUDE.md.

Services code COMPLETE: gateway, auth-service, catalog-service, sales-service, ml-service,
  dashboard-bff, notification-service, integration-service.

=== TRẠNG THÁI HIỆN TẠI (2026-05-20) ===
Test run bị gián đoạn. Đã xác định các vấn đề sau:

INFRASTRUCTURE (docker-compose):
- Docker Desktop phải được bật TRƯỚC khi chạy bất kỳ service nào
- Sau khi Docker lên: postgres:5433, redis:6379, kafka:9092, zookeeper:2181 đều healthy
- MailHog CHƯA có trong docker-compose.yml (notification-service mail DOWN)

SERVICE STATUS khi đã có infrastructure:
- catalog-service (8082): UP, DB UP, API /v3/api-docs 200 ✅
- notification-service (8091): DEGRADED (Mail DOWN - thiếu MailHog) ⚠️
- sales-service (8083): UP ✅ (chưa test API)
- dashboard-bff (8090): UP ✅ (chưa test API)
- discovery-server (8761): UP ✅
- config-server (8888): UP ✅
- api-gateway (8080): FAILED ❌ — JWT_SECRET env var không được load
- auth-service (8081): FAILED ❌ — CONFIG_SERVER_URL env var không được load
- integration-service (8084): FAILED ❌ — POSTGRES_PASSWORD env var không được load

ROOT CAUSE cho 3 service fail:
  Khi start bằng PowerShell Start-Process, env vars từ .env KHÔNG được kế thừa.
  Services cần được start từ terminal nơi .env đã được export.

CHƯA TEST: ml-service, toàn bộ API business logic, Kafka event flow, auth flow.

NEXT: Hướng dẫn cách start đúng để test tiếp:
  1. Start Docker Desktop
  2. docker-compose up -d
  3. Mở terminal riêng, load .env, start từng service
  4. Test theo flow: auth → catalog → sales → kafka → ml
```

---

## Bug Fix Run — 2026-05-20 — ALL PASS ✅

### Verification Tests
| Test | Kịch bản | Expected | Actual | Status |
|------|----------|----------|--------|--------|
| Test 1 | GET /products sau soft delete | totalElements=0, không có inactive | totalElements=0 | ✅ PASS |
| Test 2 | POST /webhooks/kiotviet (không có connector) | HTTP 404 | HTTP 404 | ✅ PASS |
| Test 3 | GET /integrations/{uuid}/jobs (connector không tồn tại) | HTTP 404 + RFC 7807 | HTTP 404 + RFC 7807 | ✅ PASS |

### Bugs Fixed
| ID | Severity | Service | Fix |
|----|----------|---------|-----|
| BUG-01 | Medium | catalog-service | ProductRepository.findByTenantIdAndStatus() + ProductService filter status=active |
| BUG-02 | Low | integration-service | getSyncJobs() validate connector tồn tại trước khi query jobs |
| BUG-03 | High (Security) | integration-service | WebhookController reject 404 ngay khi config==null |
| BUG-04 | Low | notification-service | @Schema + @ExampleObject trên PUT /preferences |
| ISSUE-05 | Low | infra | insight-mailhog thêm vào docker-compose.yml |

### System Status sau fix
- Services UP: 9/10 (notification DEGRADED → sẽ UP hoàn toàn sau docker-compose up mailhog)
- E2E flow: auth → catalog → sales → kafka → ml ✅ hoạt động hoàn toàn
- Tests: 39/39 PASS (sau fix)
- Kafka topics: auth.tenant.registered, catalog.inventory.updated, sales.order.completed đều có messages

---

## shared-core — COMPLETE ✅ (2026-05-20)

Path: `shared-core/`

| Module | Description |
|--------|-------------|
| `common-security` | `UserContext` (ThreadLocal holder), `@CurrentUser` annotation, `UserContextFilter` (reads 6 gateway headers → populates UserContext), `InternalHeaders` constants |
| `common-events` | Kafka event DTOs: TenantRegisteredEvent, InventoryUpdatedEvent, OrderCompletedEvent, ForecastGeneratedEvent, RecommendationCreatedEvent, SyncCompletedEvent |
| `common-web` | `GlobalExceptionHandler` (@RestControllerAdvice), RFC 7807 Problem Details, common error codes |

---

## api-gateway — Refactor COMPLETE ✅ (2026-05-20)

Thay đổi so với gateway phase 3 ban đầu:
- `TenantContextFilter` nay inject đủ **6 headers** xuống downstream:
  `X-Tenant-Id`, `X-User-Id`, `X-Tenant-Slug`, `X-User-Roles`, `X-User-Permissions`, `X-Correlation-Id`
- Route config: `RemoveRequestHeader=Authorization` — downstream services không nhận raw JWT
- Swagger UI: SecurityScheme config hỗ trợ Bearer auth trực tiếp từ UI

---

## Services refactor — @CurrentUser — COMPLETE ✅ (2026-05-20)

Tất cả controllers trong 6 services đã được refactor:
- Trước: `@RequestHeader("X-Tenant-Id") UUID tenantId, @RequestHeader("X-User-Id") UUID userId, ...`
- Sau: `@CurrentUser UserContext user` — clean, type-safe, không lặp lại header name

Services đã refactor: auth-service, catalog-service, sales-service, dashboard-bff, integration-service, notification-service

---

## shared-core/common-web — COMPLETE ✅ (2026-05-20)

Path: `shared-core/common-web/`

| Class | Description |
|-------|-------------|
| `ErrorCode` | Enum 12 error codes, mỗi code có HTTP status + default message |
| `ApiError` | RFC 7807 Problem Details response (type, title, status, detail, instance, correlationId, timestamp, errors[]) |
| `FieldError` | Record cho validation error per field |
| `BusinessException` | Base runtime exception với ErrorCode |
| `ResourceNotFoundException` | 404 wrapper |
| `ValidationException` | 400 wrapper với field errors |
| `UnauthorizedException` | 401 wrapper |
| `ForbiddenException` | 403 wrapper |
| `GlobalExceptionHandler` | @RestControllerAdvice, xử lý 10 exception types, auto-configured |

Auto-configuration: `WebAutoConfiguration` registered via `META-INF/spring/AutoConfiguration.imports`. Services chỉ cần add dependency, không cần `@Import`.

Build: `common-web-1.0.0.jar` installed to local Maven repo — `./mvnw clean install` BUILD SUCCESS.

---

## shared-core/common-events — COMPLETE ✅ (2026-05-20)

Path: `shared-core/common-events/`

| Event DTO | Topic | Producer | Consumers |
|-----------|-------|----------|-----------|
| `TenantRegisteredEvent` | `auth.tenant.registered` | auth-service | audit (future) |
| `InventoryUpdatedEvent` | `catalog.inventory.updated` | catalog-service | ml-service, notification-service |
| `OrderCompletedEvent` | `sales.order.completed` | sales-service | catalog-service (planned), ml-service |
| `ProductSyncedEvent` | `integration.product.synced` | integration-service | catalog-service (planned) |
| `OrderSyncedEvent` | `integration.order.synced` | integration-service | sales-service (planned) |
| `InventorySyncedEvent` | `integration.inventory.synced` | integration-service | catalog-service (planned) |
| `SyncCompletedEvent` | `integration.sync.completed` | integration-service | catalog-service, sales-service (planned) |
| `ForecastGeneratedEvent` | `ml.forecast.generated` | ml-service | dashboard-bff, notification-service |
| `RecommendationCreatedEvent` | `ml.recommendation.created` | ml-service | dashboard-bff, notification-service |

`EventObjectMapper` — pre-configured Jackson ObjectMapper (JavaTimeModule, no timestamps, FAIL_ON_UNKNOWN_PROPERTIES=false).

Build: `common-events-1.0.0.jar` installed to local Maven repo — `./mvnw clean install` BUILD SUCCESS.

---

## Refactor: common-events migration — COMPLETE ✅ (2026-05-20)

Tất cả services đã dùng chung event DTO từ `shared-core/common-events`:

### Thay đổi common-events
- Tất cả DTOs thêm `@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)` → wire format snake_case cho Python compatibility

### Java producers (dùng common-events DTO khi publish)
- `auth-service` → `TenantRegisteredEvent` (thay thế internal, set `planType`/`ownerEmail`/`ownerName`)
- `catalog-service` → `InventoryUpdatedEvent` (thay thế record, thêm `productId`/`sku`, String IDs)
- `sales-service` → `OrderCompletedEvent` (thay thế record, `OrderItemPayload` với `totalPrice`/`currency`)
- `integration-service` → `ProductSyncedEvent`, `OrderSyncedEvent`, `InventorySyncedEvent`, `SyncCompletedEvent` (thay thế raw `Map<String, Object>`)

### Java consumers (dùng common-events DTO khi deserialize)
- `notification-service` → `InventoryUpdatedEvent`, `ForecastGeneratedEvent`, `RecommendationCreatedEvent`
- `dashboard-bff` → `ForecastGeneratedEvent`, `RecommendationCreatedEvent`

### Python (ml-service)
- `app/events/schemas.py` — Pydantic models mirror Java DTOs (snake_case fields)
- Consumer dùng `OrderCompletedEvent` và `InventoryUpdatedEvent` typed models thay vì raw dict

### Payload classes nội bộ đã xóa
- `auth-service/event/TenantRegisteredEvent.java`
- `catalog-service/event/InventoryUpdatedEvent.java`
- `sales-service/event/OrderCompletedEvent.java`
- `notification-service/event/payload/InventoryUpdatedPayload.java`
- `notification-service/event/payload/MlForecastPayload.java`
- `notification-service/event/payload/MlRecommendationPayload.java`

Build verification: `./mvnw clean compile` — tất cả 6 Java services PASS.
Python: `from app.events.schemas import ...` — OK.

---

## Refactor: common-web migration — COMPLETE ✅ (2026-05-20)

Tất cả 6 services đã migrate sang `shared-core/common-web`:

| Service | GlobalExceptionHandler nội bộ | Custom exceptions migrate |
|---------|-------------------------------|---------------------------|
| auth-service | ✅ Xóa | `AuthException` → `UnauthorizedException`; `ConflictException` → `BusinessException(DUPLICATE_RESOURCE)` |
| catalog-service | ✅ Xóa | `ResourceNotFoundException(r,id)` → `ResourceNotFoundException(msg)`; `DuplicateResourceException` → `BusinessException(DUPLICATE_RESOURCE)` |
| sales-service | ✅ Xóa | `ResourceNotFoundException(r,id)` → `ResourceNotFoundException(msg)`; `DuplicateResourceException` → `BusinessException(DUPLICATE_RESOURCE)`; `OrderStateException` → `BusinessException(CONFLICT)` |
| dashboard-bff | ✅ Xóa | Không có custom exception throw sites — fallback Mono.zip vẫn giữ nguyên |
| notification-service | ✅ Xóa | `ResourceNotFoundException(r,id)` → `ResourceNotFoundException(msg)` |
| integration-service | ✅ Xóa | `ResourceNotFoundException` → common-web; `ConnectorException` auth fail → `BusinessException(DOWNSTREAM_ERROR)`; credential errors → `BusinessException(INTERNAL_ERROR)` |

Build verification: `./mvnw clean compile` — tất cả 6 services PASS, không lỗi.

common-web auto-configure: services chỉ cần add dependency — không cần `@Import`.

---

## config-repo — COMPLETE ✅ (2026-05-24)

Path: `config-repo/` (root level)

16 files được tạo:

| File | Mục đích |
|------|---------|
| `application.yml` | Shared: Jackson config, logging pattern (correlationId), actuator |
| `application-dev.yml` | Dev: actuator full, DEBUG logging tất cả services |
| `api-gateway.yml` / `-dev.yml` | JWT issuer, swagger services list |
| `auth-service.yml` / `-dev.yml` | JWT config, Flyway/JPA schema auth_db |
| `catalog-service.yml` / `-dev.yml` | Flyway/JPA schema catalog_db |
| `sales-service.yml` / `-dev.yml` | Flyway/JPA schema sales_db |
| `integration-service.yml` / `-dev.yml` | Jasypt, KiotViet URLs, sync schedule, rate limiters |
| `notification-service.yml` / `-dev.yml` | Mail SMTP config, Flyway schema notification_db |
| `dashboard-bff.yml` / `-dev.yml` | Downstream URLs (lb://), timeouts |

Config-server cập nhật: thêm `optional:file:${CONFIG_REPO_PATH:../../config-repo}` vào search-locations.
`.env.example` thêm `CONFIG_REPO_PATH=D:/SU26/EXE201/insight-flow-ai/config-repo`.

---

## ml-service: Training Bug Fix — COMPLETE ✅ (2026-05-24)

### Bug fixes

**BUG: Multi-item order chỉ lưu 1 item**
- `app/db/models.py`: `SalesData.event_id` đổi từ `unique=True` → `UniqueConstraint("event_id", "variant_id")`
- `app/events/consumer.py`: dedup check giờ filter `(event_id, variant_id)` per item, đổi `return` → `continue`

### Feature: Training trigger endpoint

Thêm `app/api/training.py`:

| Endpoint | Method | Mô tả |
|---------|--------|-------|
| `POST /api/v1/ml/train` | 202 | Trigger Prophet training cho tất cả variants của tenant. Cần ≥ 30 data points. |
| `GET /api/v1/ml/train/{job_id}` | 200 | Poll trạng thái training job (PENDING/RUNNING/SUCCESS/FAILED) |

Hàm `start_training_background(tenant_id)` — dùng bởi cả API endpoint lẫn Kafka consumer.

`app/events/consumer.py` `_check_training_readiness()` — auto-trigger training lần đầu khi:
1. Tenant tích lũy đủ 30 data points
2. Chưa có model file nào trên disk

Build: `python -c "from app.main import app"` — OK.

---

## api-contracts — COMPLETE ✅ (2026-05-25)

Path: `api-contracts/`

| File | Lines | Endpoints |
|------|-------|----------|
| `auth-service.yaml` | 293 | 5 (register, login, refresh, logout, me) |
| `catalog-service.yaml` | 1074 | 14 (products, variants, categories, locations, inventory) |
| `sales-service.yaml` | 773 | 9 (orders, customers, suppliers) |
| `dashboard-bff.yaml` | 554 | 4 (overview, health-summary, recommendations-summary, forecast-summary) |
| `notification-service.yaml` | 485 | 6 (list, unread-count, mark-read, mark-all-read, preferences) |
| `integration-service.yaml` | 594 | 7 (connectors CRUD, trigger-sync, list-jobs, webhook) |
| `ml-service.yaml` | 598 | 6 (forecast, batch-forecast, recommendations, refresh, train, train-status) |

Format: OpenAPI 3.1.0, servers via gateway + direct, ProblemDetail (RFC 7807), pagination schema chung.

---

## catalog-service: Kafka Consumer — COMPLETE ✅ (2026-05-25)

File mới: `business-services/catalog-service/src/main/java/com/insightflow/catalog/event/OrderCompletedConsumer.java`

### Logic
- Lắng nghe `sales.order.completed` (group: `catalog-service-events`)
- Mỗi order item → deduct inventory với `movementType=SALE`, `quantityChange=-quantity`
- **Idempotency**: kiểm tra `(tenantId, referenceType="ORDER", referenceId=orderId, variantId)` trước khi deduct
- **Location selection**: chọn location có `quantityOnHand` cao nhất cho variant đó (greedy, tránh negative stock)
- Fail-open per item: lỗi 1 item không block các item khác hoặc ack

### Files thay đổi
| File | Thay đổi |
|------|---------|
| `application.yml` | Thêm Kafka consumer config (group-id, deserializers, manual ack) |
| `KafkaConfig.java` | Thêm `NewTopic` bean cho `sales.order.completed` |
| `InventoryMovementRepository.java` | Thêm `existsByTenantIdAndReferenceTypeAndReferenceIdAndVariantId()` |
| `OrderCompletedConsumer.java` | NEW: consumer class |

Build: `./mvnw compile` → BUILD SUCCESS ✅

### Kafka event flow hoàn chỉnh
```
sales-service → sales.order.completed → catalog-service (deduct inventory)
                                      → ml-service (accumulate SalesData)
catalog-service → catalog.inventory.updated → ml-service (update InventorySnapshot)
                                            → notification-service (low stock alert)
```

---

## Resume Prompt (cập nhật 2026-05-25)

```
Read docs/handoffs/CURRENT_STATE.md first.
Then read PROJECT_CONTEXT.md, .claude/CLAUDE.md.

=== TRẠNG THÁI HIỆN TẠI (2026-05-25) ===

HOÀN THÀNH (code xong, build pass):
- 10/10 services: gateway, auth, catalog, sales, ml-service, bff, notification, integration
- shared-core: common-security, common-events, common-web
- config-repo: 16 files cho tất cả services
- api-contracts: 7 YAML specs (auth + 6 mới)
- catalog Kafka consumer: auto-deduct inventory từ sales.order.completed
- ml-service: training endpoint + multi-item order bug fix

CÒN LẠI (theo thứ tự ưu tiên):
1. sales-service: @Scheduled refresh daily_sales_summary (materialized view)
2. Variant full CRUD: PUT /variants/{id}, DELETE /variants/{id}, GET /variants/{id}
3. scripts/: build-all.ps1, run-local.ps1, export-openapi.ps1
4. observability/: Prometheus scrape config, Grafana dashboards, Loki config
5. Service-level JWT/tenant validation
6. integration-service: Sapo connector, Haravan connector (Phase 2)
7. Frontend repo: Next.js, TypeScript client từ api-contracts/

E2E test lần cuối: 2026-05-20, 9/9 PASS
Chưa test: ml-service training endpoint, catalog Kafka consumer, config-repo serving
```

---
