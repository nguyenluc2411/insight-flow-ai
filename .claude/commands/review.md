---
description: Perform a thorough code review with focus on Insight Flow AI conventions, security, and architecture compliance.
---

# /review

Review code with attention to Insight Flow AI specific concerns.

## Scope

If user provides specific files/diff, review only those. Otherwise:
- Review currently uncommitted changes (`git diff` and `git diff --cached`)
- If nothing pending, ask user which scope to review

## Review Checklist

### 1. Architecture Compliance
- [ ] Service stays within its bounded context (no cross-service DB joins)
- [ ] Communication via events/API, not shared databases
- [ ] New services not introduced without approval in `PROJECT_CONTEXT.md`
- [ ] Layering respected (controller → service → repository → entity)
- [ ] No business logic in controllers
- [ ] No SQL/repository calls from controllers

### 2. Multi-Tenancy (CRITICAL)
- [ ] All new business entities have `tenant_id UUID NOT NULL`
- [ ] All queries filter by `tenant_id` (check repository methods)
- [ ] Indexes include `tenant_id` for unique constraints
- [ ] `TenantContext` is set/cleared properly (no leaks across threads)
- [ ] Service methods receive or read `tenantId` explicitly

### 3. Security
- [ ] No secrets/credentials hardcoded
- [ ] No sensitive data in logs (passwords, tokens, PII)
- [ ] Input validation at DTO layer (Jakarta Bean Validation)
- [ ] SQL injection protection (parameterized queries, no string concat)
- [ ] Authentication required for non-public endpoints
- [ ] Authorization checked at service layer (not just at gateway)
- [ ] CORS not set to `*` with credentials

### 4. Database
- [ ] Migration file naming follows `V{N}__{description}.sql`
- [ ] No modifications to already-merged migrations
- [ ] Foreign keys have explicit `ON DELETE` behavior
- [ ] Required indexes present (FK, tenant_id, frequently filtered columns)
- [ ] JSONB used appropriately (not for frequently-filtered fields)

### 5. API Contract Discipline
- [ ] OpenAPI annotations present (`@Operation`, `@ApiResponse`)
- [ ] If public API changed: `api-contracts/*.yaml` updated in same change
- [ ] Versioning rule applied if breaking change
- [ ] Response shape consistent (pagination, error format)
- [ ] RFC 7807 Problem Details for errors

### 6. Events
- [ ] Event DTOs in `shared-core/common-events/` if reused
- [ ] Event name follows `{domain}.{entity}.{action}` convention
- [ ] Required fields present: `event_id`, `tenant_id`, `occurred_at`
- [ ] Consumers are idempotent (use `event_id` for deduplication)

### 7. Code Quality
- [ ] Lombok annotations used appropriately (not over-applied)
- [ ] No commented-out code
- [ ] No `TODO` without ticket reference
- [ ] Method/class size reasonable (flag classes >300 lines, methods >50 lines)
- [ ] Names are descriptive (no `data`, `temp`, `x1`)
- [ ] Tests added for new logic (or explained why not)

### 8. ML-Specific (if reviewing ml-service)
- [ ] Model artifacts NOT committed
- [ ] Tenant isolation in model paths
- [ ] Phase 1 boundaries respected (no deep learning)
- [ ] Cold-start case handled
- [ ] Confidence intervals or rule reasons in responses

### 9. Gateway-Specific (if reviewing api-gateway)
- [ ] Reactive stack maintained (no `spring-boot-starter-web`)
- [ ] No `.block()` calls
- [ ] No DB calls
- [ ] Filter order correct (rate limit → JWT → tenant context)

## Output Format

Provide review as:

```markdown
## Review Summary

**Files reviewed**: <count>
**Critical issues**: <count>
**Warnings**: <count>
**Suggestions**: <count>

## Critical Issues 🚨
(Block merge — must fix)
- **File:line** - Description of issue + suggested fix

## Warnings ⚠️
(Should fix before merge)
- **File:line** - Description

## Suggestions 💡
(Nice to have)
- **File:line** - Description

## Architectural Concerns
(If any cross-cutting issues)

## What's Good ✅
(Acknowledge well-done parts briefly)
```

## Delegation

If review touches multiple domains:
- Note which agent owns each domain
- Suggest specific agents to consult for deep dive
- Don't try to be expert in every domain — flag and defer
