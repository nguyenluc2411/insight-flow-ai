package com.insightflow.security;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Arrays;

/**
 * AOP aspect that enforces {@link RequiresRole} on controller methods.
 *
 * <p>Reads the caller's {@link UserContext} from {@link UserContextHolder}
 * (populated by {@link UserContextFilter} from gateway-injected headers) and throws:
 * <ul>
 *   <li>HTTP 401 when no security context is present (request bypassed the gateway)</li>
 *   <li>HTTP 403 when none of the required roles is in {@code UserContext.roles()}</li>
 * </ul>
 *
 * <p>Registered automatically by {@link SecurityAutoConfiguration}.
 */
@Aspect
@Slf4j
public class RoleAspect {

    @Around("@annotation(requiresRole)")
    public Object enforce(ProceedingJoinPoint pjp, RequiresRole requiresRole) throws Throwable {
        UserContext ctx = UserContextHolder.get();
        if (ctx == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Authentication required");
        }

        boolean allowed = ctx.roles() != null
                && Arrays.stream(requiresRole.value()).anyMatch(ctx.roles()::contains);

        if (!allowed) {
            log.warn("Role denied: userId={} required={} actual={}",
                    ctx.userId(), Arrays.toString(requiresRole.value()), ctx.roles());
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Requires role: " + String.join(" or ", requiresRole.value()));
        }

        return pjp.proceed();
    }
}
