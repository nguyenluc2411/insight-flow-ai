# Gateway Implementation State
Updated: 2026-05-16T00:00:00Z

## Completed

- [x] Phase 3.1: CorrelationIdFilter (order -100) — committed `51878c8`
      Generates/propagates X-Correlation-Id. Writes to Reactor Context for MDC bridge.
- [x] Phase 3.2: LoggingFilter (order -50) — committed `1d06424`
      Entry/exit logging. Masks sensitive query params. Uses doFinally().
- [x] Phase 3.3: JwtAuthenticationFilter (order 100) + JwtValidator — committed `b56f175`
      JJWT 0.12.6. Skips public routes via route metadata.public. RFC 7807 401 on failure.
      Dev secret updated to Base64: `aW5zaWdodGZsb3ctZGV2LTMyYnl0ZXMtc2VjcmV0ISE=`
      (= "insightflow-dev-32bytes-secret!!", 32 bytes, HS256-safe)
- [x] Phase 3.4: TenantContextFilter (order 200) — committed `5f56c98`
      Strips + re-adds X-Tenant-Id/X-User-Id/X-Tenant-Slug/X-User-Roles from JWT claims.
- [x] Phase 3.5: RateLimitConfig + FixedWindowRateLimiter — committed `078fb1d`
      Lua script fixed-window for auth routes (5 req/900s/IP).
      Token-bucket RedisRateLimiter for default (5/10) and webhook (10/30) routes.
      ipKeyResolver: X-Forwarded-For → RemoteAddress fallback.
- [x] Phase 3.6: GlobalExceptionHandler — committed `4c25322`
      Extends AbstractErrorWebExceptionHandler at @Order(-2). RFC 7807 for all errors.

## In Progress

None.

## Remaining

None — all phases complete.

## Open Issues

1. **No mvnw wrapper in api-gateway**: Used system Maven (mvn) instead.
   If CI uses `./mvnw`, add the Maven wrapper to the service.

2. **newConfig() required**: `RateLimiter<C>` in Spring Cloud 2025.0.x extends
   `Configurable<C>` which has a non-default `newConfig()`. Added override to
   `FixedWindowRateLimiter`.

3. **FixedWindowRateLimiter is NOT @Component**: Instantiated via `@Bean` in
   `RateLimitConfig`. This is intentional — the bean factory controls its config.

4. **application.yml Redis password**: Dev docker-compose runs Redis without auth.
   No password configured in application-dev.yml (correct for dev).

5. **JwtValidator reads secret as Base64**: If auth-service issues JWTs signed with the
   same Base64-decoded key, round-trip verification works. Coordinate with auth-service
   dev config to use the same secret.

## Filter Execution Order (final)

| Order | Class                    | File                          |
|-------|--------------------------|-------------------------------|
| -100  | CorrelationIdFilter      | filter/CorrelationIdFilter.java    |
|  -50  | LoggingFilter            | filter/LoggingFilter.java          |
|   0   | RequestRateLimiter (SCG) | via route filter in application.yml |
| 100   | JwtAuthenticationFilter  | filter/JwtAuthenticationFilter.java |
| 200   | TenantContextFilter      | filter/TenantContextFilter.java    |

## Public Routes (no JWT required)

- `POST /api/v1/auth/login`
- `POST /api/v1/auth/register-tenant`
- `POST /api/v1/auth/refresh`
- `POST /api/v1/webhooks/**`

## Resume Prompt

If token runs out, paste this into a new session:

"""
Read docs/handoffs/CURRENT_STATE.md first.
Then read PROJECT_CONTEXT.md and .claude/agents/gateway-agent.md.
All Gateway Component 3 phases are COMPLETE as of 2026-05-16.
Next work: auth-service implementation (auth-agent scope).
Do not re-implement any gateway filter — they are all committed.
"""
