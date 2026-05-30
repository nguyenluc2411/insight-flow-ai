\# Insight Flow AI - Backend Project Context



> \*\*Purpose\*\*: File này là context tổng cho mọi AI session (Claude, Cursor, Copilot Chat).

> Paste toàn bộ file này vào đầu mỗi conversation mới để AI hiểu dự án.

> 

> \*\*Scope\*\*: Repository này CHỈ chứa backend services. Frontend được tách ra repo riêng `insight-flow-frontend`.



\---



\## 1. Product Vision



\*\*Insight Flow AI\*\* là SaaS B2B cung cấp:

\- Dự báo nhu cầu sản phẩm thời trang theo thời gian (demand forecasting)

\- Phát hiện xu hướng từ dữ liệu social/ecommerce (trend detection)

\- Đề xuất hành động xử lý tồn kho: giảm giá, clearance, restock (recommendation engine)

\- Tối ưu chuỗi cung ứng cho shop thời trang vừa và nhỏ tại Việt Nam



\*\*Target MVP\*\*: Shop thời trang nhỏ 1-5 cửa hàng, đang dùng KiotViet/Sapo/Haravan.

\*\*Pricing dự kiến\*\*: $20-50/store/tháng (tier-based).

\*\*North star metric\*\*: Tỷ lệ shop giảm được dead stock sau 3 tháng dùng.



\## 2. Repository Structure



Dự án chia làm 2 repo riêng biệt:



| Repo | Scope | Tech |

|------|-------|------|

| `insight-flow-ai` (này) | Tất cả backend services, ML, infra | Java, Python, Docker |

| `insight-flow-frontend` (riêng) | Web app, admin dashboard | React/Next.js |



\*\*Liên kết giữa 2 repo\*\*:

\- Backend expose OpenAPI 3.1 spec qua `/v3/api-docs` mỗi service

\- OpenAPI specs commit vào folder `api-contracts/` của repo này

\- Frontend pull specs về và generate TypeScript client

\- Contract-first development: thay đổi API → update spec → notify frontend team



\## 3. Team



\- 2 Backend (Java/Spring) — repo này

\- 1 ML Engineer (Python) — repo này

\- 1 Fullstack (Frontend + Integration) — chủ yếu repo frontend, hỗ trợ integration ở backend



\## 4. Tech Stack (Backend Only)



| Layer | Technology |

|-------|-----------|

| Backend services | Java 21, Spring Boot 3.5.x, Spring Cloud 2024.x |

| Service discovery | Eureka |

| Config | Spring Cloud Config |

| Gateway | Spring Cloud Gateway |

| ML services | Python 3.11, FastAPI, scikit-learn, Prophet, XGBoost |

| Messaging | Apache Kafka |

| Database | PostgreSQL 16 (DB-per-service via schema) |

| Cache | Redis 7 |

| Object storage | MinIO (S3-compatible) cho models, images |

| Container | Docker Compose (dev), Kubernetes (production future) |

| CI/CD | GitHub Actions |

| Observability | Prometheus + Grafana + Loki |

| API doc | SpringDoc OpenAPI 3.1 |



\## 5. Architecture Principles



1\. \*\*Microservices nhưng tối giản cho MVP\*\*: 8-10 services, không phải 20+. Tách service khi có lý do cụ thể (scale, team, tech khác).

2\. \*\*Multi-tenancy\*\*: Shared database + `tenant\_id` column. Postgres Row Level Security (RLS) làm defense in depth. KHÔNG dùng schema-per-tenant hay DB-per-tenant ở giai đoạn này.

3\. \*\*Event-driven\*\*: Kafka là backbone. Services giao tiếp async qua events khi có thể, sync qua REST chỉ khi bắt buộc.

4\. \*\*Database-per-service\*\*: Mỗi service có Postgres schema riêng. KHÔNG cross-service join. Cần data của service khác → gọi API hoặc subscribe event.

5\. \*\*API-first\*\*: Mọi service expose OpenAPI 3.1 spec. BFF aggregate cho frontend repo consume.

6\. \*\*Plugin-based integrations\*\*: 1 `integration-service` với plugin pattern, không tạo service riêng cho mỗi POS.

7\. \*\*Contract-first cho frontend\*\*: Thay đổi API public phải update OpenAPI spec trước, notify frontend repo.



\## 6. Services (MVP Scope)



\### Platform Services (`platform-services/`)

\- `discovery-server` - Eureka registry ✅ DONE

\- `config-server` - Spring Cloud Config ✅ DONE

\- `api-gateway` - Routing, JWT auth, rate limiting, CORS cho frontend repo 🔄 IN PROGRESS



\### Business Services (`business-services/`)

\- `auth-service` - Tenants, users, roles, JWT issuance

\- `catalog-service` - Products, variants, locations, inventory levels (gộp product/inventory/warehouse)

\- `sales-service` - Orders, order items, customers, suppliers (gộp supplier vì nhỏ)



\### Intelligence Services (`intelligence-services/`)

\- `ml-service` - Forecasting, recommendation (Python/FastAPI). Phase 1: Prophet + rule-based recommendation.



\### Integration Services (`integration-services/`)

\- `integration-service` - Plugin-based POS connectors (KiotViet, Sapo, Haravan)



\### Engagement Services (`engagement-services/`)

\- `dashboard-bff` - Backend for Frontend, aggregate APIs cho frontend repo

\- `notification-service` - Email/Zalo/in-app alerts



\*\*KHÔNG làm trong MVP\*\* (defer to Phase 2+):

\- ❌ trend-intelligence-service (NLP từ social)

\- ❌ feature-engineering-service riêng

\- ❌ model-registry MLflow riêng (dùng file-based ban đầu)

\- ❌ report-service riêng (dashboard-bff lo)

\- ❌ Service riêng cho từng connector

\- ❌ supplier-service riêng (gộp vào sales-service)

\- ❌ warehouse-service riêng (gộp vào catalog-service)



\## 7. Folder Structure (Backend Monorepo)



```

insight-flow-ai/

├── .github/

│   └── workflows/

│       ├── ci-java.yml             # Build all Java services

│       └── ci-python.yml           # Build ML service

│

├── platform-services/

│   ├── discovery-server/           ✅ Eureka

│   ├── config-server/              ✅ Spring Cloud Config

│   └── api-gateway/                🔄 Spring Cloud Gateway

│

├── business-services/

│   ├── auth-service/               📋 Tenants, users, JWT

│   ├── catalog-service/            📋 Products + inventory + warehouses

│   └── sales-service/              📋 Orders + customers + suppliers

│

├── intelligence-services/

│   └── ml-service/                 📋 Python FastAPI - forecast + recommendation

│

├── integration-services/

│   └── integration-service/        📋 Plugin-based, KiotViet first

│

├── engagement-services/

│   ├── dashboard-bff/              📋 BFF cho frontend repo

│   └── notification-service/       📋 Email/Zalo/in-app

│

├── shared-core/                    # Java shared libraries

│   ├── common-events/              📋 Kafka event DTOs

│   ├── common-security/            📋 JWT validator, TenantContext

│   └── common-web/                 📋 Exception handlers, Problem Details

│

├── infrastructure/

│   ├── docker/

│   │   └── docker-compose.yml      ✅ Kafka, Redis, Postgres

│   ├── kafka/                      📋 Topic init scripts

│   └── postgres/

│       └── init.sql                📋 Create schemas

│

├── observability/

│   ├── prometheus/                 📋 Scrape config

│   ├── grafana/                    📋 Dashboards JSON

│   └── loki/                       📋 Log aggregation config

│

├── config-repo/                    📋 Spring Cloud Config source files

│   ├── application.yml             # Shared config (db url pattern, kafka)

│   ├── auth-service.yml

│   ├── catalog-service.yml

│   └── ...

│

├── api-contracts/                  📋 OpenAPI specs cho frontend repo

│   ├── auth-service.yaml

│   ├── catalog-service.yaml

│   └── dashboard-bff.yaml

│

├── scripts/                        📋 Build, deploy, dev scripts

│   ├── build-all.ps1

│   ├── run-local.ps1

│   └── export-openapi.ps1

│

├── docs/

│   ├── architecture/               📋 ADR, diagrams

│   ├── api/                        📋 API guides

│   └── runbooks/                   📋 On-call docs

│

├── .gitignore                      ✅

├── CLAUDE.md                       ✅ AI assistant instructions

├── PROJECT\_CONTEXT.md              ✅ File này

└── README.md                       ✅

```



\*\*Folder đã xóa (không dùng MVP)\*\*:

\- ❌ `event-streaming/` → gộp vào `shared-core/common-events/`

\- ❌ `testing/` → tests ở trong từng service

\- ❌ `devops/` → gộp vào `infrastructure/`



\## 8. Database Strategy



\- Mỗi service 1 schema PostgreSQL riêng (không phải DB instance riêng cho MVP)

\- Naming convention: `{service\_name}\_db` schema, ví dụ `auth\_db`, `catalog\_db`

\- Migration: Flyway cho mỗi service

\- Mọi bảng nghiệp vụ có: `id UUID`, `tenant\_id UUID`, `created\_at`, `updated\_at`

\- Mọi bảng sync với POS có thêm: `external\_ids JSONB`, `raw\_data JSONB`, `source VARCHAR`

\- Inventory dùng pattern append-only `inventory\_movements` table (event sourcing)



\## 9. Authentication \& Authorization



\- \*\*JWT\*\*: Access token (15 min) + Refresh token (30 days, hashed in DB để revoke được)

\- \*\*Access token claims\*\*: `sub`, `tenant\_id`, `tenant\_slug`, `plan`, `roles\[]`, `permissions\[]`

\- \*\*Gateway\*\*: Authentication only (verify JWT signature + expiry)

\- \*\*Services\*\*: Authorization (check permissions + tenant\_id match)

\- \*\*Default roles\*\*: OWNER, MANAGER, STAFF, ACCOUNTANT, VIEWER

\- \*\*Tenant context propagation\*\*: Header `X-Tenant-Id`, `X-User-Id` xuống downstream services

\- \*\*CORS\*\*: Gateway whitelist domain của frontend repo (`localhost:3000` dev, production domain sau)



\## 10. Kafka Events (Core Set)



Naming: `{domain}.{entity}.{action}` - ví dụ `catalog.inventory.updated`



| Topic | Producer | Consumers |

|-------|----------|-----------|

| `catalog.product.created` | catalog-service | ml-service, dashboard-bff |

| `catalog.inventory.updated` | catalog-service | ml-service, notification-service |

| `sales.order.completed` | sales-service | catalog-service, ml-service |

| `integration.sync.completed` | integration-service | catalog-service, sales-service |

| `ml.forecast.generated` | ml-service | dashboard-bff, notification-service |

| `ml.recommendation.created` | ml-service | dashboard-bff, notification-service |



Event format: JSON với schema (chuyển Avro sau khi stable).

Mọi event có: `event\_id`, `event\_type`, `tenant\_id`, `occurred\_at`, `payload`, `metadata`.



\## 11. Integration Strategy



\### Priority order

1\. \*\*KiotViet\*\* (làm trước, ship production trước khi làm cái khác)

2\. Sapo

3\. Haravan  

4\. Shopify (chỉ khi có customer yêu cầu)



\### Sync tiers

1\. \*\*Webhook\*\* (real-time, best effort, idempotency key)

2\. \*\*Incremental poll\*\* (15 min, từ last\_sync watermark)

3\. \*\*Full reconciliation\*\* (daily 2-4 AM, alert nếu drift)



\### Connector framework

```

integration-services/integration-service/

├── core/ (Registry, Interface, Orchestrator, RateLimiter, CredentialVault)

├── connectors/

│   ├── kiotviet/

│   ├── sapo/

│   └── haravan/

└── webhook/

```



\## 12. ML Approach (Phase 1)



\*\*Keep it simple, ship value\*\*:

\- Forecasting: Prophet với category-level fallback cho cold start

\- Recommendation: Rule-based đầu tiên ("tồn >90 ngày + sales giảm >50% → clearance")

\- KHÔNG dùng LSTM, KHÔNG train deep learning models ở Phase 1

\- Model storage: file system + MinIO, chưa cần MLflow



\## 13. Current Progress



\### Done ✅

\- Backend monorepo Git initialized

\- Folder structure refactored (xóa event-streaming, testing, devops)

\- `.gitignore` cleaned up

\- `discovery-server` (Eureka) running

\- `config-server` running

\- Docker Compose: Kafka, Redis, PostgreSQL up

\- `PROJECT\_CONTEXT.md`, `CLAUDE.md` ready

\- `shared-core` libraries: `common-web`, `common-security`, `common-events`



\### In Progress 🔄

\- `api-gateway`



\### Next Up 📋

\- `auth-service` (multi-tenancy foundation)

\- `catalog-service` skeleton

\- KiotViet connector spike



\## 14. Coding Conventions



\### Java/Spring

\- Package: `com.insightflow.{service}` (vd: `com.insightflow.auth`)

\- Layered: `controller` → `service` → `repository` → `entity`

\- DTOs riêng cho request/response, KHÔNG expose entity ra API

\- Mapper: MapStruct

\- Validation: Jakarta Bean Validation ở DTO

\- Exception: Centralized handler với `@RestControllerAdvice`, return RFC 7807 Problem Details

\- Lombok cho boilerplate (`@Data`, `@Builder`, `@RequiredArgsConstructor`)

\- Repository: Spring Data JPA, custom queries với `@Query` hoặc JPQL

\- OpenAPI: SpringDoc, mọi endpoint có `@Operation`, `@ApiResponse`



\### Database

\- Tên bảng: `snake\_case`, số nhiều (`users`, `products`)  

\- Mọi FK có index

\- UUID v4 cho primary key

\- Migration: `V{version}\_\_{description}.sql` (Flyway convention)



\### Git

\- Branch: `feature/{ticket}-{short-desc}`, `fix/...`, `chore/...`

\- Commit: Conventional Commits (`feat:`, `fix:`, `chore:`, `docs:`)

\- PR: 1 service per PR khi có thể



\### API Contract Discipline

\- Breaking change API → bump version (vd: `/api/v1` → `/api/v2`)

\- Update OpenAPI spec trong cùng PR với code change

\- Notify frontend team qua channel chung khi spec đổi



\## 15. Anti-patterns To Avoid



\- ❌ Tạo service mới khi chưa có lý do scale/team riêng

\- ❌ Cross-service database join hoặc shared schema

\- ❌ Sync REST call khi có thể dùng event

\- ❌ Quên `tenant\_id` trong query (sẽ leak data giữa tenants)

\- ❌ Lưu credentials POS plaintext (phải encrypt với KMS hoặc Jasypt)

\- ❌ Webhook handler không idempotent

\- ❌ Train ML model phức tạp trước khi có data thật

\- ❌ Breaking change API mà không bump version + notify frontend team

\- ❌ Đưa logic UI/UX vào BFF (BFF chỉ aggregate + transform data)

\- ❌ Commit `target/`, `.idea/`, `\*.iml` vào Git



\## 16. When Asking AI For Help



Provide:

1\. Service bạn đang code (vd: auth-service)

2\. Layer/file cụ thể (controller, service, repository, schema)  

3\. Yêu cầu cụ thể (implement endpoint X, refactor Y, debug Z)

4\. Snippet code hiện tại nếu có

5\. Error message đầy đủ nếu debug



Avoid:

\- "Build cho tôi cả service" → quá rộng, dễ sai context

\- Hỏi mà không reference file/folder cụ thể

\- Đề cập đến frontend code (đó là repo khác, scope khác)



\## 17. shared-core — Hướng Dẫn Sử Dụng



`shared-core` là Java multi-module library được build thành jar và import vào các business service.

**Bước đầu tiên (một lần duy nhất):** install vào local Maven repo:

```bash
cd shared-core && mvn install
```



\### Dependency cho từng module



\#### common-web — Exception handling + RFC 7807

Dùng cho: mọi servlet-based Java service (auth, catalog, sales, dashboard-bff...)

```xml
<dependency>
    <groupId>com.insightflow</groupId>
    <artifactId>common-web</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

\- `GlobalExceptionHandler` tự đăng ký qua Spring Boot auto-configuration — không cần làm gì thêm.

\- Throw `ResourceNotFoundException`, `TenantAccessException`, `ConflictException`, hoặc `BusinessException` từ bất kỳ layer nào — handler tự convert sang đúng RFC 7807 response.



\#### common-security — TenantContext propagation

Dùng cho: mọi service cần đọc tenantId/userId từ request (tức là tất cả business services).

```xml
<dependency>
    <groupId>com.insightflow</groupId>
    <artifactId>common-security</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

\- `TenantContextFilter` tự đăng ký qua auto-configuration — đọc `X-Tenant-Id` và `X-User-Id` từ gateway headers.

\- Trong service layer, gọi `TenantContextHolder.requireTenantId()` để lấy tenantId hiện tại.

\- Trong repository, LUÔN filter theo tenantId:

```java
// ✅ Đúng
List<Product> findByTenantId(UUID tenantId);

// ❌ Sai — leak data giữa tenants
List<Product> findAll();
```



\#### common-events — Kafka event DTOs

Dùng cho: service nào produce hoặc consume Kafka events.

```xml
<dependency>
    <groupId>com.insightflow</groupId>
    <artifactId>common-events</artifactId>
    <version>0.0.1-SNAPSHOT</version>
</dependency>
```

\- Dùng `EventTopics.*` constants khi khai báo `@KafkaListener(topics = ...)` và khi gọi `KafkaTemplate.send(...)`.

\- Mỗi event class implement `InsightFlowEvent`, dùng `@Builder` để tạo:

```java
// Producer (catalog-service)
var event = CatalogProductCreatedEvent.builder()
        .tenantId(tenantId)
        .productId(product.getId())
        .name(product.getName())
        .sku(product.getSku())
        .category(product.getCategory())
        .status("ACTIVE")
        .build();
kafkaTemplate.send(EventTopics.CATALOG_PRODUCT_CREATED, event.getTenantId().toString(), event);

// Consumer (ml-service)
@KafkaListener(topics = EventTopics.CATALOG_PRODUCT_CREATED, groupId = "ml-service")
public void onProductCreated(CatalogProductCreatedEvent event) { ... }
```



\### Dependency graph

```
auth-service      → common-web + common-security
catalog-service   → common-web + common-security + common-events
sales-service     → common-web + common-security + common-events
integration-service → common-web + common-security + common-events
dashboard-bff     → common-web + common-security
ml-service        → common-events (Python — không dùng Java libs)
notification-service → common-web + common-security + common-events
```

