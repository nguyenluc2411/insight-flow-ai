package com.insightflow.security;

import java.lang.annotation.*;

/**
 * Declares the role(s) required to invoke a controller method.
 *
 * <p>Access is granted when the caller's {@link UserContext#roles()} contains
 * ANY of the listed role names. Enforced at runtime by {@link RoleAspect},
 * which throws HTTP 401 (no context) or HTTP 403 (role absent).
 *
 * <p>Example:
 * <pre>{@code
 * @GetMapping("/admin/tenants")
 * @RequiresRole("SUPER_ADMIN")
 * public Page<AdminTenantListItem> list(...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresRole {
    /** One or more role names; access is granted if the caller has any of them. */
    String[] value();
}
