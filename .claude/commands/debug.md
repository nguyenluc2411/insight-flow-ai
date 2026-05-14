---
description: Systematically debug an issue in Insight Flow AI, with domain-aware investigation paths.
---

# /debug

Debug an issue methodically. Avoid jumping to conclusions.

## Required Information

Before starting, ensure you have:
1. **Symptom**: What's actually happening (error message, unexpected behavior)
2. **Expected**: What should happen instead
3. **Reproduction**: Steps to reproduce or when it occurs
4. **Scope**: Which service(s)/component(s) affected
5. **Recent changes**: What was modified recently (deploys, config, code)

If any missing, ASK before investigating. Don't guess.

## Investigation Protocol

### Step 1: Classify the issue type

Identify which category applies:

| Category | Symptoms | First place to look |
|----------|----------|---------------------|
| **Auth/Permission** | 401, 403, "Unauthorized" | JWT claims, gateway logs, auth-service logs |
| **Routing** | 404, "No instance available" | Gateway config, Eureka registration |
| **Data integrity** | Wrong values, missing data | DB queries, transaction logs, recent migrations |
| **Performance** | Slow response, timeouts | Query plans, N+1, missing indexes |
| **Tenancy leak** | User sees other tenant's data | Repository filters, RLS policies, TenantContext |
| **Event flow** | Downstream effects missing | Kafka consumer lag, dead-letter queue, idempotency keys |
| **ML output wrong** | Bad forecasts/recs | Data quality, model version, feature drift |
| **Startup failure** | Service won't start | Config-server connection, DB migrations, port conflicts |

### Step 2: Delegate to appropriate agent if scoped

If the issue clearly belongs to one domain:
- Auth issues → `auth-agent`
- Gateway issues → `gateway-agent`
- ML issues → `ai-agent`
- DB/query issues → `database-agent`
- Contract/CORS → `frontend-agent`

If cross-domain, root coordinates.

### Step 3: Evidence-first investigation

Follow this order strictly:
1. **Read the error message carefully** — don't assume
2. **Check logs** of the failing component (use correlation ID to trace)
3. **Verify configuration** (env vars, config-repo files)
4. **Reproduce locally** if possible
5. **Form hypothesis** based on evidence
6. **Test hypothesis** with minimal change
7. **Only then propose fix**

### Step 4: Common Pitfalls to Check

#### Spring Boot
- Profile not activated (`SPRING_PROFILES_ACTIVE`)
- Config-server unreachable on startup
- Wrong port / port conflict
- Eureka not registered (check `/actuator/health`)
- Circular dependency

#### Multi-tenancy
- TenantContext not propagated across async boundaries
- ThreadLocal not cleared (memory leak / data leak)
- Repository not extending tenant-aware base
- Direct entity manager use bypassing filter

#### Gateway
- Filter order wrong (e.g., JWT before rate limit)
- Reactive blocking (using blocking client in WebFlux)
- CORS preflight not handled
- Route to wrong service ID (case-sensitive Eureka name)

#### Database
- Missing index causing seq scan
- N+1 from lazy loading
- Wrong connection pool size
- Transaction propagation misconfigured
- Migration order issue

#### Kafka
- Consumer group rebalance loop
- Offset commit failure
- Idempotency not handled → duplicate processing
- Schema evolution incompatibility

#### ML
- Model version mismatch with feature pipeline
- Stale training data
- Feature drift (data distribution changed)
- Missing tenant_id in prediction (model loaded for wrong tenant)

## Output Format

```markdown
## Debug Report

### Issue Classification
**Category**: [from table above]
**Likely owner**: [agent name or root]

### Evidence Collected
1. <observation 1>
2. <observation 2>

### Hypothesis
<Most likely cause based on evidence>

### Verification Steps
1. <how to confirm hypothesis>

### Proposed Fix
<Specific change to make, with file:line>

### Prevention
<How to prevent this in the future — test, monitor, code change>
```

## Anti-Patterns to Avoid

- ❌ "Let me try restarting it" before understanding
- ❌ Adding logging without hypothesis (random shotgun debugging)
- ❌ Fixing symptoms without root cause (will recur)
- ❌ Reverting changes without identifying what broke
- ❌ "It works on my machine" without checking environment differences

## When to Escalate to User

- Production incident with data integrity impact (don't experiment)
- Issue requires deleting data
- Fix involves schema change in running system
- Cause unclear after 30 minutes of investigation
