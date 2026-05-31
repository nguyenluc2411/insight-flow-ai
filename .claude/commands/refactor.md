---
description: Safely refactor code in Insight Flow AI, respecting service boundaries and architectural constraints.
---

# /refactor

Refactor code with safety, scope discipline, and architectural awareness.

## Refactor Categories

Identify which type of refactor is requested:

| Type | Scope | Risk | Approach |
|------|-------|------|----------|
| **Local** | Within a single class/method | Low | Direct change + test |
| **Service-internal** | Within one service | Medium | Plan changes, run service tests |
| **Cross-service** | Multiple services | High | Plan + coordinate agents + careful staging |
| **Architectural** | Changes patterns/structure | Very high | Requires `PROJECT_CONTEXT.md` update first |

## Pre-Refactor Checklist

Before touching code:

1. **Confirm scope** — what's IN, what's OUT
2. **Identify owner** — which agent's domain (if scoped)
3. **Check tests exist** — refactoring without tests is dangerous
4. **Read related code** — understand intent before changing
5. **Verify it's actually needed** — sometimes code that "looks bad" works for a reason

## When NOT to Refactor

Skip refactoring if:
- ❌ Code works and isn't actively maintained (don't break working code)
- ❌ Tests don't exist and writing them isn't in scope
- ❌ The change touches >3 services without a clear feature need
- ❌ It's purely stylistic disagreement (not measurable improvement)
- ❌ User asked for a feature, not a refactor (focus on the feature)

## Refactor Patterns Common in This Repo

### Extract Service Layer
Symptom: Controller has business logic.
```java
// BEFORE - logic in controller
@PostMapping("/products")
public ResponseEntity<?> create(@RequestBody ProductDto dto) {
    if (productRepo.findBySku(dto.getSku()).isPresent()) { ... }
    var product = new Product();
    product.setSku(dto.getSku());
    // ... lots of logic
    productRepo.save(product);
    kafkaTemplate.send("catalog.product.created", event);
    return ResponseEntity.ok(...);
}

// AFTER - delegated to service
@PostMapping("/products")
public ResponseEntity<ProductResponse> create(
    @Valid @RequestBody CreateProductRequest req
) {
    var product = productService.create(req, TenantContext.getTenantId());
    return ResponseEntity.status(CREATED).body(productMapper.toResponse(product));
}
```

### Add Tenant Awareness
Symptom: Query doesn't filter `tenant_id`.
```java
// BEFORE - dangerous, returns any tenant's data
List<Product> findByCategoryId(UUID categoryId);

// AFTER - tenant-safe
List<Product> findByTenantIdAndCategoryId(UUID tenantId, UUID categoryId);
```

### Replace Cross-Service DB Query with Event
Symptom: Service A queries Service B's database directly.
```java
// BEFORE - violates DB-per-service
// In catalog-service:
@Query("SELECT s FROM sales_db.sales_orders s WHERE ...") // WRONG
List<SalesOrder> findRecentSales(...);

// AFTER - event-driven sync
// In catalog-service:
@KafkaListener(topics = "sales.order.completed")
public void onOrderCompleted(SalesOrderEvent event) {
    // Update local cached projection
    productSalesStatsRepo.updateStats(event.productId(), event.quantity());
}
```

### Extract Shared Code to common-*
Symptom: Same code in multiple services.
- If it's truly cross-cutting (auth, tenant, events) → move to `shared-core/`
- If similar but not identical → leave duplicated (cheap is better than wrong abstraction)

## Refactor Workflow

### Phase 1: Plan (do NOT modify code yet)

```markdown
## Refactor Plan

**Goal**: <what improves after refactor>
**Scope**: <files/services affected>
**Owner agent(s)**: <which agents need to be involved>
**Risk level**: <low/medium/high>

### Current State
<brief description of how code is now>

### Target State
<how code will be after>

### Steps
1. <step 1>
2. <step 2>
...

### Tests
- <existing tests that protect this code>
- <new tests needed before refactor>
- <test plan to verify behavior preserved>

### Rollback
<how to undo if it goes wrong>
```

Present the plan to the user. Wait for approval before Phase 2.

### Phase 2: Execute (small steps)

1. Add tests FIRST if missing
2. Make smallest viable change
3. Run tests after each change
4. Commit each logical step separately (easier to bisect)
5. Don't mix refactor with feature changes — pure refactor commits

### Phase 3: Verify

- All tests pass
- API contract unchanged (verify against `api-contracts/*.yaml`)
- No new TODOs introduced
- Performance equal or better (for hot paths)
- Cross-service contracts respected (events, REST)

## Anti-Patterns to Avoid

- ❌ "While I'm in here, let me also fix..." — scope creep
- ❌ Renaming everything to one's preferred style
- ❌ Introducing new patterns mid-refactor (do one thing at a time)
- ❌ Refactoring without running tests
- ❌ Breaking public API as side effect of internal refactor
- ❌ Mixing refactor with bug fixes (which one caused the test failure?)

## When to Escalate to Root / User

- Refactor would change `PROJECT_CONTEXT.md` decisions
- Refactor introduces new architectural pattern
- Refactor touches `shared-core/` (affects all services)
- Refactor requires schema migration
- Refactor cannot be done backward-compatibly

## Delegation

For scoped refactors:
- Within auth → `auth-agent`
- Within gateway → `gateway-agent`
- Within ML → `ai-agent`
- Schema-level → `database-agent`
- API shape → `frontend-agent`

For cross-cutting refactors, root coordinates and invokes agents per service.
