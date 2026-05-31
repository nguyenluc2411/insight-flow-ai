# CLAUDE.md - Engineering Context for Insight Flow AI

> **Purpose**: Hướng dẫn Claude Code và các AI assistant cách làm việc trong monorepo này.
> 
> **Pair with**: `PROJECT_CONTEXT.md` (ở root) chứa product/business context. File này focus vào engineering workflow.

---

## 1. Project Overview

**Insight Flow AI** là SaaS B2B forecast nhu cầu thời trang + đề xuất xử lý tồn kho cho shop VN.

- **Architecture**: Microservices (Java/Spring Boot + Python/FastAPI)
- **Communication**: Event-driven via Kafka, REST khi bắt buộc
- **Multi-tenancy**: Shared DB + `tenant_id` (Postgres RLS làm defense in depth)
- **Repo này chứa**: Backend services, ML services, infra (Docker, observability)
- **Frontend**: Tách repo riêng `insight-flow-frontend` (KHÔNG nằm trong repo này)

Đọc `PROJECT_CONTEXT.md` ở root trước khi bắt đầu task lớn để hiểu product vision.

## 2. Monorepo Architecture

```
insight-flow-ai/
├── platform-services/          # Infra services (discovery, config, gateway)
├── business-services/          # Domain services (auth, catalog, sales)
├── intelligence-services/      # ML services (Python)
├── integration-services/       # POS connectors (KiotViet, Sapo...)
├── engagement-services/        # BFF, notifications
├── shared-core/               # Java shared libraries
├── infrastructure/            # Docker, Kafka, Postgres setup
├── observability/             # Prometheus, Grafana, Loki
├── config-repo/               # Spring Cloud Config files
├── api-contracts/             # OpenAPI specs (export cho frontend repo)
└── .claude/                   # AI assistant configuration (file này ở đây)
```

## 3. Service Boundaries (CRITICAL)

| Service | Status | Owner Agent | Owns |
|---------|--------|-------------|------|
| `discovery-server` | ✅ Done | platform | Eureka registry |
| `config-server` | ✅ Done | platform | Spring Cloud Config |
| `api-gateway` | 🔄 In Progress | `gateway-agent` | Routing, JWT validation, CORS, rate limit |
| `auth-service` | 📋 Planned | `auth-agent` | Tenants, users, roles, JWT issuance |
| `catalog-service` | 📋 Planned | `database-agent` | Products, variants, inventory |
| `sales-service` | 📋 Planned | `database-agent` | Orders, customers, suppliers |
| `ml-service` | 📋 Planned | `ai-agent` | Forecasting, recommendations |
| `integration-service` | 📋 Planned | (no dedicated agent) | POS connectors |
| `dashboard-bff` | 📋 Planned | (no dedicated agent) | API aggregation |
| `notification-service` | 📋 Planned | (no dedicated agent) | Email/Zalo/in-app |

**Rule**: Mỗi service có 1 owner agent rõ ràng (hoặc generic). KHÔNG có chuyện 2 agent cùng modify 1 service.

## 4. Coding Conventions

### Java/Spring Boot
- **Java version**: 21 (dùng record, sealed, pattern matching khi phù hợp)
- **Spring Boot**: 3.5.x, Spring Cloud 2024.x
- **Package**: `com.insightflow.{service}` (vd: `com.insightflow.auth`)
- **Layered architecture**: `controller` → `service` → `repository` → `entity`
- **DTO discipline**: 
  - Request/Response DTO riêng, KHÔNG expose entity ra API
  - Dùng MapStruct cho mapper
  - Validation: Jakarta Bean Validation ở DTO layer
- **Exception handling**: 
  - `@RestControllerAdvice` centralized
  - Return RFC 7807 Problem Details
- **Lombok**: Cho phép `@Data`, `@Builder`, `@RequiredArgsConstructor`, `@Slf4j`
- **OpenAPI**: SpringDoc, mọi endpoint phải có `@Operation` + `@ApiResponse`

### Python (ML services only)
- **Python**: 3.11+
- **Framework**: FastAPI
- **Structure**: `app/{api,services,models,utils}`
- **Format**: `black` + `ruff`
- **Type hints**: bắt buộc trên public functions
- **Pydantic**: cho mọi request/response model

### Database
- **Naming**: `snake_case`, plural cho table (`users`, `products`)
- **Primary key**: UUID v4
- **Mọi bảng nghiệp vụ phải có**: `id`, `tenant_id`, `created_at`, `updated_at`
- **Foreign keys**: phải có index
- **Migrations**: Flyway, naming `V{version}__{description}.sql`
- **Multi-tenancy**: MỌI query phải filter `tenant_id` (enforced ở repository layer)

## 5. Spring Boot Service Conventions

### Standard structure per service
```
{service-name}/
├── src/main/java/com/insightflow/{service}/
│   ├── {Service}Application.java
│   ├── config/                 # @Configuration classes
│   ├── controller/             # REST controllers
│   ├── service/                # Business logic
│   ├── repository/             # JPA repositories
│   ├── entity/                 # JPA entities
│   ├── dto/                    # Request/Response DTOs
│   │   ├── request/
│   │   └── response/
│   ├── mapper/                 # MapStruct mappers
│   ├── exception/              # Custom exceptions
│   └── event/                  # Kafka producers/consumers
├── src/main/resources/
│   ├── application.yml
│   ├── application-dev.yml
│   ├── application-prod.yml
│   └── db/migration/           # Flyway scripts
├── src/test/
├── Dockerfile
├── pom.xml
└── README.md
```

### Required dependencies per service type
- **All services**: `spring-boot-starter-web`, `spring-boot-starter-actuator`, `eureka-client`, `config-client`
- **Data services**: `spring-boot-starter-data-jpa`, `postgresql`, `flyway-core`
- **Event producers/consumers**: `spring-kafka`
- **Gateway only**: `spring-cloud-starter-gateway` (reactive, NO `spring-boot-starter-web`)

## 6. AI Workflow Conventions

### Khi nào delegate sang subagent

Claude root nên delegate khi task rơi vào domain rõ ràng:

| Task example | Delegate to |
|--------------|-------------|
| "Sửa JWT validation" | `auth-agent` |
| "Tune forecast model" | `ai-agent` |
| "Add rate limit config" | `gateway-agent` |
| "Design table schema" | `database-agent` |
| "Update OpenAPI spec cho frontend" | `frontend-agent` |
| "Refactor toàn repo" | KHÔNG delegate - root tự handle, gọi agents từng phần |
| "Add comment" / "Fix typo" | KHÔNG delegate - root tự làm |

### Khi NÀO Claude root tự làm

- Task cross-domain (vd: end-to-end feature qua nhiều service)
- Task nhỏ (< 20 dòng code, không cần specialized context)
- Code review tổng thể
- Architecture decisions
- Khi user hỏi câu mở (planning, discussion)

### Delegation principles

1. **One agent, one domain**: Không gọi 2 agent cùng lúc trong 1 task
2. **Pass context tối thiểu**: Đưa file path + yêu cầu cụ thể, không paste cả `PROJECT_CONTEXT.md`
3. **Verify output**: Sau khi agent return, root verify trước khi commit
4. **Escalate khi unclear**: Agent không tự ý expand scope - hỏi root nếu task ambiguous

## 7. Git Conventions

- **Branch naming**: 
  - `feature/{ticket}-{short-desc}` (vd: `feature/AUTH-12-jwt-refresh`)
  - `fix/{ticket}-{short-desc}`
  - `chore/{short-desc}`
  - `docs/{short-desc}`
- **Commit format**: Conventional Commits
  - `feat(auth): add refresh token endpoint`
  - `fix(gateway): handle JWT expiry properly`
  - `chore(ci): update workflow file`
  - `docs(readme): update setup instructions`
- **PR scope**: 1 service per PR khi có thể. Cross-service PR phải có context rõ trong description.
- **Never commit**: `target/`, `.idea/`, `*.iml`, `.env`, secrets

## 8. Rules for Modifying Services

### Always
- ✅ Update OpenAPI spec trong cùng PR khi đổi public API
- ✅ Add migration script khi đổi schema (KHÔNG sửa migration cũ đã merge)
- ✅ Update event schema trong `shared-core/common-events` khi đổi Kafka payload
- ✅ Filter `tenant_id` trong MỌI query nghiệp vụ
- ✅ Test rate limiting + JWT khi đổi Gateway routing
- ✅ Encrypt credentials (Jasypt/KMS), KHÔNG bao giờ plaintext

### Never
- ❌ Cross-service database joins (gọi API hoặc consume event thay vì query DB service khác)
- ❌ Shared schema giữa services
- ❌ Sync REST call khi event-driven phù hợp hơn
- ❌ Breaking API change mà không bump version
- ❌ Tạo service mới khi chưa được approve trong PROJECT_CONTEXT.md
- ❌ Hardcode tenant_id, secrets, URLs trong code

## 9. Agent Delegation Guide

### Available agents

- **`auth-agent`**: Authentication, authorization, JWT, RBAC, multi-tenancy enforcement
- **`gateway-agent`**: API Gateway routing, filters, CORS, rate limiting
- **`ai-agent`**: ML models, forecasting, recommendation logic, feature engineering
- **`database-agent`**: Schema design, migrations, query optimization, indexing
- **`frontend-agent`**: API contracts, OpenAPI specs, CORS config (serves frontend repo)

### Decision tree

```
Task arrives
    │
    ├── Is it within a single agent's domain?
    │       └── YES → Delegate to that agent
    │       └── NO  → ↓
    │
    ├── Is it cross-domain feature (>2 services)?
    │       └── YES → Root coordinates, calls agents sequentially per service
    │       └── NO  → ↓
    │
    ├── Is it architectural/planning?
    │       └── YES → Root handles directly (consult PROJECT_CONTEXT.md)
    │       └── NO  → ↓
    │
    └── Default: Root handles directly
```

## 10. Clean Architecture Best Practices

1. **Dependency direction**: Outer layers depend on inner (controller → service → repository). Never reverse.
2. **No business logic in controllers**: Controllers are thin - validate, delegate to service, return response.
3. **Services are tenant-aware**: Every service method that touches data takes `tenantId` as parameter (or reads from `TenantContext`).
4. **Repositories return entities**: Mappers convert to DTOs at service layer.
5. **Domain events first**: When state changes, publish event before returning to caller.
6. **Idempotency**: Webhook handlers, event consumers MUST be idempotent (use `event_id` deduplication).
7. **Fail-fast validation**: Validate at DTO boundary, not in service layer.
8. **Configuration over code**: Routing, feature flags, thresholds → config files, not hardcoded.

## 11. Environment: Windows Development Notes

**Context**: The project lives on Windows (`D:\SU26\EXE201\insight-flow-ai`), but Claude Code executes shell commands through a Unix-like shell (Git Bash / WSL). These two environments have incompatible path conventions — always follow the Unix rules below.

### Path Conventions (CRITICAL)

| | Example | Result |
|---|---|---|
| ❌ WRONG | `ls D:\SU26\EXE201\insight-flow-ai\business-services\` | Parse error — backslash is escape char in Bash |
| ✅ CORRECT | `ls business-services/` | Works — CWD is already project root |
| ❌ WRONG | `cd D:\SU26\EXE201\insight-flow-ai\platform-services` | Same parse error |
| ✅ CORRECT | `cd platform-services/` | Works cross-platform |

**Rules**:
- Always use **Unix-style relative paths** from project root
- **Never** include drive letters (`D:\`) or backslashes (`\`) in Bash/tool paths
- Forward slashes work on both Windows and Unix; backslashes do not

### Why This Matters

- Bash treats `\` as an escape character, so `D:\path` silently parses as `D:` + escape sequences
- The working directory is always set to the project root (`D:\SU26\EXE201\insight-flow-ai`) — no absolute path is ever needed
- Relative forward-slash paths work identically in Git Bash, WSL, and CI (Linux)

### Exceptions

- PowerShell scripts under `scripts/*.ps1` may use Windows-style paths internally — that is intentional and acceptable
- When invoking PowerShell via the PowerShell tool, Windows paths are fine

### Tool Usage

- **Read / Edit / Write / Glob / Grep tools**: use Unix-style paths, e.g. `business-services/auth-service/pom.xml`
- **Bash tool**: use relative Unix paths, e.g. `cd platform-services/api-gateway && ./mvnw clean install`
- **Never** pass a drive letter or backslash to any tool

---

## 12. Quick Reference

### Common file locations
- Multi-tenancy filter: `shared-core/common-security/`
- Kafka event DTOs: `shared-core/common-events/`
- Exception handlers base: `shared-core/common-web/`
- OpenAPI exports: `api-contracts/{service-name}.yaml`
- DB init: `infrastructure/postgres/init.sql`
- Spring Config files: `config-repo/{service-name}.yml`

### Common ports (dev)
- Discovery: `8761`
- Config: `8888`
- Gateway: `8080`
- Auth: `8081`
- Catalog: `8082`
- Sales: `8083`
- Integration: `8084`
- ML service: `8000` (Python convention)
- BFF: `8090`

### Useful commands
```bash
# Build single service
cd platform-services/api-gateway && ./mvnw clean install

# Run with dev profile
./mvnw spring-boot:run -Dspring-boot.run.profiles=dev

# Export OpenAPI spec
./mvnw springdoc-openapi:generate
```

## 13. When You're Stuck

1. **Check PROJECT_CONTEXT.md** for product decisions already made
2. **Check this file** for engineering conventions
3. **Check agent-specific .md** in `.claude/agents/` for domain rules
4. **Ask the user** if requirements are ambiguous - don't guess
5. **Look at completed services** (`discovery-server`, `config-server`) for working patterns
