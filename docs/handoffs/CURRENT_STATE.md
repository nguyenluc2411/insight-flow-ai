# Implementation State — Insight Flow AI Backend
Updated: 2026-05-16T00:00:00Z

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
- `POST /api/v1/auth/register-tenant` → 201 (via api-gateway public route)
- `POST /api/v1/auth/login`           → 200 (via api-gateway public route)
- `POST /api/v1/auth/refresh`         → 200 (via api-gateway public route)
- `POST /api/v1/auth/logout`          → 204 (via api-gateway public route)
- `GET  /api/v1/auth/me`              → 200 (protected — requires gateway JWT)

### Key design decisions
- BCrypt cost 12 (≈250ms verify time per auth-agent spec)
- Refresh tokens stored as SHA-256 hash; raw UUID returned to client
- Auth failures always return "Invalid credentials" (never reveals email existence)
- tenant_id enforced at repository query level (findByTenantIdAndEmail)
- JWT claims: sub, tenant_id, tenant_slug, plan, roles[], permissions[] (matches gateway validator)
- Kafka topic `auth.tenant.registered` auto-created (3 partitions)

### Open issues
1. **Spring Security not added**: auth-service has NO Spring Security config.
   The service relies on the api-gateway for JWT validation.
   Endpoints are unprotected at the service level (only protected at gateway).
   For production, add Spring Security + stateless JWT filter to the service.
2. **PasswordService uses BCryptPasswordEncoder directly** (not Spring Security PasswordEncoder bean).
   Works correctly; no impact on functionality. Can be refactored when Spring Security is added.
3. **Kafka fail-open**: If Kafka is down, TenantRegisteredEvent publish silently fails.
   Tenant registration still succeeds. Add outbox pattern later for guaranteed delivery.
4. **No @Transactional on logout**: If DB fails mid-logout, token not revoked. Acceptable for MVP.
5. **roles EAGER fetch on User**: All user roles+permissions loaded on every login.
   Fast for MVP (<5 roles per user). Add pagination/lazy load if role count grows.

---

## Infrastructure — COMPLETE ✅

| Service | Port | Status |
|---------|------|--------|
| docker-compose (Postgres, Redis, Kafka, ZK) | various | With healthchecks |
| discovery-server (Eureka) | 8761 | Done |
| config-server | 8888 | Done |
| api-gateway | 8080 | Done |
| auth-service | 8081 | Done |

---

## Next Steps

### auth-service (short-term)
- [ ] Add Spring Security with stateless JWT filter for service-level protection
- [ ] Implement password reset (token-based, 15min TTL)
- [ ] Add user invitation flow

### catalog-service (next service to build)
- [ ] Product, variant, category, inventory entities
- [ ] Tenant-isolated CRUD

### Sales + Integration services (after catalog)

---

## Startup Order

```
1. docker-compose up -d           (Redis, Postgres, Kafka)
2. ./mvnw spring-boot:run          (discovery-server, port 8761)
3. ./mvnw spring-boot:run          (config-server, port 8888)
4. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (api-gateway, port 8080)
5. ./mvnw spring-boot:run -Dspring-boot.run.profiles=dev  (auth-service, port 8081)
```

---

## Test the End-to-End Flow

```bash
# Register tenant
curl -X POST http://localhost:8080/api/v1/auth/register-tenant \
  -H "Content-Type: application/json" \
  -d '{"tenantName":"Test Shop","slug":"test-shop",
       "ownerEmail":"owner@test.com","ownerPassword":"Test1234!",
       "ownerFullName":"Shop Owner"}'
# Expected: 201 with accessToken + refreshToken

# Login
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"tenantSlug":"test-shop","email":"owner@test.com","password":"Test1234!"}'
# Expected: 200 with accessToken + refreshToken

# Get current user (use accessToken from login)
curl http://localhost:8080/api/v1/auth/me \
  -H "Authorization: Bearer <accessToken>"
# Expected: 200 with user info
```

---

## Resume Prompt

```
Read docs/handoffs/CURRENT_STATE.md first.
Then read PROJECT_CONTEXT.md, .claude/CLAUDE.md.
Gateway and auth-service are COMPLETE as of 2026-05-16.
Next: catalog-service (database-agent scope for schema, auth-agent principles for tenant isolation).
Do not re-implement completed services.
```
