# Implementation State — Insight Flow AI Backend
Updated: 2026-05-18T00:00:00Z

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
- `SwaggerServiceProperties.java` — alias→serviceId mapping from YAML
- `SwaggerDocsProxyController.java` — GET /v3/api-docs/{alias} → lb://service/v3/api-docs
- `WebClientConfig.java` — immutable load-balanced WebClient
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

| Service | Port | Status |
|---------|------|--------|
| docker-compose (Postgres, Redis, Kafka, ZK) | various | With healthchecks |
| discovery-server (Eureka) | 8761 | Done |
| config-server | 8888 | Done |
| api-gateway | 8080 | Done |
| auth-service | 8081 | Done |
| catalog-service | 8082 | Done |

---

## Startup Order

```
1. docker-compose up -d           (Redis, Postgres, Kafka)
2. ./mvnw spring-boot:run          (discovery-server, port 8761)
3. ./mvnw spring-boot:run          (config-server, port 8888)
4. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (api-gateway, port 8080)
5. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (auth-service, port 8081)
6. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (catalog-service, port 8082)
```

---

## Next Steps

### catalog-service (short-term)
- [ ] Add service-level JWT/tenant validation (currently relies on gateway)
- [ ] Implement Category CRUD endpoints
- [ ] Implement ProductVariant CRUD endpoints
- [ ] Consumer for `sales.order.completed` → adjust inventory

### sales-service (next major service)
- [ ] Orders, order items, customers, suppliers
- [ ] Event: `sales.order.completed` → consumed by catalog-service

---

## Resume Prompt

```
Read docs/handoffs/CURRENT_STATE.md first.
Then read PROJECT_CONTEXT.md, .claude/CLAUDE.md.
Gateway, auth-service, and catalog-service are COMPLETE as of 2026-05-18.
Next: sales-service or catalog-service enhancements (category/variant endpoints).
Do not re-implement completed services.
```
