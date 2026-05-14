---
agent: auth-agent
paths:
  - "business-services/auth-service/**"
  - "shared-core/common-security/**"
  - "config-repo/auth-service.yml"
  - "api-contracts/auth-service.yaml"
keywords:
  - "JWT issuance"
  - "JWT signing"
  - "refresh token"
  - "tenant onboarding"
  - "user registration"
  - "password reset"
  - "RBAC"
  - "role permission"
  - "TenantContext"
  - "multi-tenancy enforcement"
priority: high
---

# Auth Routing Rule

This rule routes tasks related to authentication, authorization, and multi-tenancy to the `auth-agent`.

## When This Rule Applies

A task should be delegated to `auth-agent` when ANY of the following is true:

1. **File paths match**: Changes happen within paths listed above
2. **Keywords appear**: User mentions any keyword listed (case-insensitive)
3. **Intent matches**: User asks about user identity, token management, or tenant isolation logic

## Examples That Match

✅ "Implement password reset flow"  
✅ "Add a new role with permission to view forecasts"  
✅ "JWT refresh isn't working — debug it"  
✅ "Set up tenant onboarding endpoint"  
✅ "How should I encrypt user PII in auth-service?"  
✅ "Add audit log for login attempts"  

## Examples That DO NOT Match (Despite Looking Similar)

❌ "Add JWT validation to api-gateway" → routes to `gateway-agent` (validation is gateway's job)  
❌ "Add permission check in catalog-service endpoint" → handled by the service's owner; auth-agent only owns AuthN, not AuthZ enforcement at every service  
❌ "Frontend login form not working" → wrong repo, inform user  

## Cross-Domain Coordination Required When

- **JWT structure changes** → coordinate with `gateway-agent` (they validate)
- **New table needs RLS policy** → coordinate with `database-agent` (they own DDL)
- **Auth endpoint added/changed** → update OpenAPI spec, coordinate with `frontend-agent`

## Delegation Behavior

When this rule triggers:
1. Root reads the task and identifies it as auth-related
2. Root invokes `auth-agent` with:
   - Specific task description
   - Relevant file paths (not full repo dump)
   - Any cross-domain constraints (e.g., "must work with current gateway validation")
3. Root waits for agent output
4. Root verifies output against:
   - PROJECT_CONTEXT.md decisions (multi-tenancy strategy, etc.)
   - Other affected agents' constraints
5. Root commits or asks user for confirmation

## Prevent Cross-Domain Contamination

The `auth-agent` MUST NOT:
- Modify gateway routing → that's `gateway-agent`
- Modify business service controllers (catalog, sales) — even if "auth check needed"
- Add ML logic to scoring user risk — that's `ai-agent`
- Touch frontend code — wrong repo

If task requires the above, `auth-agent` should:
- Complete its own portion
- Document what other agents/services need to change
- Return control to root for coordination
