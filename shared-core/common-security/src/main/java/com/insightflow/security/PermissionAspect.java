package com.insightflow.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * AOP aspect that enforces {@link RequiresPermission} on controller methods.
 *
 * <p>Reads the caller's {@link UserContext} from {@link UserContextHolder} (populated
 * by {@link UserContextFilter} from gateway-injected headers) and throws:
 * <ul>
 *   <li>HTTP 401 when no security context is present (request bypassed the gateway)</li>
 *   <li>HTTP 403 when the required permission is not in {@code UserContext.permissions()}</li>
 * </ul>
 *
 * <p>Registered automatically by {@link SecurityAutoConfiguration}.
 */
@Aspect
@Slf4j
public class PermissionAspect {

    @Around("@annotation(requiresPermission)")
    public Object enforce(ProceedingJoinPoint pjp, RequiresPermission requiresPermission) throws Throwable {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        String required = requiresPermission.value();
        if (!ctx.permissions().contains(required)) {
            log.warn("Permission denied: userId={} tenantId={} required={} actual={}",
                    ctx.userId(), ctx.tenantId(), required, ctx.permissions());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Missing permission: " + required);
        }

        return pjp.proceed();
    }
}
