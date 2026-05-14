# Rate Limiting Strategy

**Decision date**: 2026-05-15  
**Status**: Accepted

---

## Context

All traffic enters through a single API Gateway. The rate-limit filter runs at
order 0 — before JWT validation (order 100). At the time rate-limit decisions
are made, no JWT has been validated and no tenant or user claims are available.
IP address is the only structurally safe key at this layer.

Per-user and per-tenant quotas are business rules tied to billing plans and
service-specific SLOs. They belong in downstream services that have that context,
not at the gateway edge.

---

## Decision: Three-Layer Design

| Layer | Where | Key | Limit | Phase |
|-------|-------|-----|-------|-------|
| L1 Anti-DDoS | API Gateway (Redis token bucket) | IP address | 5 rps general; 10 rps webhooks; 5 req / 15 min login | Now |
| L2 Per-user | Each downstream service (Resilience4j `@RateLimiter`) | `X-User-Id` header | TBD per service SLO | Phase 1.5 |
| L3 Per-tenant quota | Each downstream service | `X-Tenant-Id` + billing plan | TBD per billing tier | Phase 2 |

---

## L1 — Gateway Anti-DDoS (current)

### Why IP-only at the gateway

**Structural reason**: The JWT validation filter runs at order 100. The rate-limit
filter runs at order 0. When the rate-limit decision is made, the JWT has not
been validated and no tenant/user claims have been extracted. Using a claim-based
key before claims are verified would require either (a) parsing an unverified JWT
— a security anti-pattern — or (b) reordering filters so JWT runs first, which
means unauthenticated requests bypass rate limiting entirely (a DDoS vector).

**Architectural reason**: Per-user and per-tenant quotas are business rules. They
depend on subscription tier, billing state, and service-specific SLOs — none of
which the gateway knows or should know. Encoding them at the gateway creates
coupling between the gateway and the billing domain.

### Bean reference

All gateway rate limiting is backed by Redis so token-bucket state survives
gateway restarts and is shared across horizontally scaled gateway instances.

| Bean | Type | Config | Purpose |
|------|------|--------|---------|
| `defaultRateLimiter` | `RedisRateLimiter` (token bucket) | replenishRate=5, burstCapacity=10 | All general routes (~300 req/min sustained, 600 burst) |
| `webhookRateLimiter` | `RedisRateLimiter` (token bucket) | replenishRate=10, burstCapacity=30 | POS webhook ingestion (burst on batch sync) |
| `loginRateLimiter` | Custom `FixedWindowRedisRateLimiter` | 5 req / 900 s / IP | Credential-stuffing protection on auth public endpoints |

Key resolver for **all routes**: `ipKeyResolver` — resolves `X-Forwarded-For`
(first value, when behind a load balancer) with fallback to `RemoteAddress`.

Bean definitions: `RateLimitConfig.java`

---

## L2 — Per-user rate limiting (Phase 1.5)

Each downstream service reads the `X-User-Id` header (propagated by the gateway
`TenantContextFilter` after JWT validation) and applies Resilience4j
`@RateLimiter` at the service layer. Limits are defined per service based on
observed SLOs and will be documented in each service's README once baselines
are established.

---

## L3 — Per-tenant quota enforcement (Phase 2)

Tenant quotas tied to billing plans (e.g. Starter: 10 000 API calls/day,
Professional: 100 000/day) are enforced in downstream services using
`tenant_id` from the `X-Tenant-Id` header. A shared `QuotaService` checks and
decrements quota. This is intentionally out of scope for the gateway to preserve
the single-responsibility principle.

---

## Consequences

**Positive**:
- Simple, auditable L1 protection with no business-logic coupling
- Login brute-force protection active from day one
- POS webhook ingestion handles burst without false positives
- Rate-limit state is durable and shared across gateway replicas

**Trade-offs**:
- IP-based keys are defeated by distributed botnets (accepted — L1 is
  anti-DDoS, not fraud prevention)
- IPv6 CIDR grouping not implemented (future improvement if needed)
- Per-user throttling of abusive authenticated users deferred to Phase 1.5

---

## Related

- `platform-services/api-gateway/src/main/java/com/insightflow/gateway/config/RateLimitConfig.java` — bean definitions
- `platform-services/api-gateway/src/main/resources/application.yml` — route-level filter wiring
- `docs/adr/gateway-circuit-breaker.md` — Phase 2 circuit breaker plan (to be written)

---

## Verification

Quick smoke tests once the gateway is running:

```bash
# L1 general route (should allow ~10 bursts then throttle)
for i in {1..15}; do curl -i http://localhost:8080/api/v1/catalog/products; done
# Expect: first ~10 return 503 (no downstream yet), 11+ return 429

# L1 login route (5 in 15min hard limit)
for i in {1..7}; do
  curl -i -X POST http://localhost:8080/api/v1/auth/login \
    -d '{"email":"test@test.com","password":"x"}' \
    -H "Content-Type: application/json"
done
# Expect: first 5 return 503, 6+ return 429 with Retry-After header

# Headers expected on 429:
# - X-RateLimit-Limit
# - X-RateLimit-Remaining: 0
# - X-RateLimit-Reset: <unix timestamp>
# - Retry-After: <seconds>
```
