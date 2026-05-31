---
agent: gateway-agent
paths:
  - "platform-services/api-gateway/**"
  - "config-repo/api-gateway.yml"
keywords:
  - "API gateway"
  - "routing rule"
  - "gateway filter"
  - "CORS"
  - "rate limiting"
  - "circuit breaker"
  - "JWT validation"
  - "tenant context propagation"
  - "correlation ID"
  - "X-Tenant-Id header"
  - "request forwarding"
  - "WebFlux filter"
priority: high
---

# Gateway Routing Rule

This rule routes tasks related to API Gateway to the `gateway-agent`.

## When This Rule Applies

A task should be delegated to `gateway-agent` when ANY of the following is true:

1. **File paths match**: Changes within `platform-services/api-gateway/**` or its config
2. **Keywords appear**: User mentions gateway, CORS, routing, rate limit, or related concepts
3. **Intent matches**: User asks about request lifecycle, ingress, or cross-cutting concerns at the edge

## Examples That Match

✅ "Add route for the new catalog endpoint"  
✅ "CORS error from frontend — fix it"  
✅ "Implement rate limiting for the login endpoint"  
✅ "Add correlation ID to all requests"  
✅ "JWT validation isn't working at gateway"  
✅ "Configure circuit breaker for the ml-service route"  
✅ "Propagate X-Tenant-Id header to downstream"  

## Examples That DO NOT Match (Despite Looking Similar)

❌ "Generate JWT for new user" → `auth-agent` (issuance, not validation)  
❌ "Check user permission before allowing action" → service's owner (authorization happens downstream)  
❌ "Add API endpoint for new feature" → service's owner agent (not gateway)  
❌ "Frontend can't reach API" → may be gateway OR network OR DNS; verify before delegating  

## Cross-Domain Coordination Required When

- **JWT structure changes** → coordinate with `auth-agent` (they issue, you validate)
- **New service added** → coordinate with that service's owner to add route + tests
- **CORS for new frontend domain** → coordinate with `frontend-agent`
- **Performance issue with rate limiting** → may need Redis infra changes

## Delegation Behavior

When this rule triggers:
1. Root identifies task as gateway-scope
2. Root invokes `gateway-agent` with:
   - Task description
   - Affected routes/filters
   - Constraint: stay reactive (WebFlux), don't introduce Servlet stack
3. `gateway-agent` works within `platform-services/api-gateway/`
4. Root verifies:
   - No DB calls added at gateway level (stateless principle)
   - No authorization logic (only authentication)
   - Filter order preserved (rate-limit → JWT → tenant context)
   - All routes documented in YAML

## Prevent Cross-Domain Contamination

The `gateway-agent` MUST NOT:
- Add business logic at gateway
- Hit databases (use only Redis for rate limit state)
- Issue JWTs → that's `auth-agent`
- Check permissions → that's downstream service
- Modify downstream service code

If task requires the above, `gateway-agent` should:
- Identify what should be done elsewhere
- Document the coordination needed
- Return to root with the boundary clarified

## Special: Reactive Stack Discipline

The gateway uses Spring WebFlux. Common mistakes to prevent:
- ❌ Adding `spring-boot-starter-web` (blocking, conflicts with reactive)
- ❌ Using `HttpServletRequest` (servlet API, doesn't work)
- ❌ Calling `.block()` on Mono/Flux (defeats reactive benefit)
- ❌ Using blocking JDBC at gateway

If gateway needs data, it should:
- Use reactive WebClient (non-blocking)
- Cache in Redis (reactive Redis client)
- Or simply forward and let downstream handle

## Special: Single Ingress Point

The gateway is the only entry point. Therefore:
- ANY new public capability needs a route here
- ANY public route needs explicit auth requirement (protected by default)
- Public routes (no auth) are exceptional and must be listed:
  - `/api/v1/auth/login`
  - `/api/v1/auth/register-tenant`
  - `/api/v1/auth/refresh`
  - `/actuator/health`
  - `/v3/api-docs/**`
  - Webhook endpoints (verified by signature, not JWT)

Adding to the public list requires explicit user approval.
