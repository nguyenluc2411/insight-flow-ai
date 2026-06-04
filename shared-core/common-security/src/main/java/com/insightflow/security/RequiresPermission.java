package com.insightflow.security;

import java.lang.annotation.*;

/**
 * Declares the permission required to invoke a controller method.
 *
 * <p>Format: {@code "resource:action"} — must match a permission seeded in
 * {@code auth_db.permissions} (e.g. {@code "catalog:read"}, {@code "sales:write"}).
 *
 * <p>Enforced at runtime by {@link PermissionAspect}, which reads the caller's
 * {@link UserContext} from {@link UserContextHolder} and throws HTTP 403 when the
 * required permission is absent from {@code UserContext.permissions()}.
 *
 * <p>Example:
 * <pre>{@code
 * @GetMapping
 * @RequiresPermission("catalog:read")
 * public Page<ProductResponse> listProducts(@CurrentUser UserContext user, ...) { ... }
 * }</pre>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequiresPermission {
    /** Permission key in {@code "resource:action"} format. */
    String value();
}
