# Implementation State — Insight Flow AI Backend
Updated: 2026-05-18T10:00:00Z

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
- [ ] Implement Category CRUD endpoints
- [ ] Implement ProductVariant CRUD endpoints
- [ ] Consumer for `sales.order.completed` → adjust inventory

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

## Resume Prompt

```
Read docs/handoffs/CURRENT_STATE.md first.
Then read PROJECT_CONTEXT.md, .claude/CLAUDE.md.
Gateway, auth-service, catalog-service, and sales-service are COMPLETE as of 2026-05-18.
All services use env vars from .env (no hardcoded credentials).
sales-service requires -Dspring-boot.run.jvmArguments=-Duser.timezone=UTC on Windows.
Next: integration-service (KiotViet connector) or catalog/sales enhancements.
Do not re-implement completed services.
```
