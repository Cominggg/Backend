package com.Coming.Backend.common.aspect;

import com.Coming.Backend.common.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Slf4j
public class LoggingAspect {

    private static final long SLOW_THRESHOLD_MS = 2000;

    @Around("execution(* com.Coming.Backend.*.service.*.*(..))")
    public Object log(ProceedingJoinPoint joinPoint) throws Throwable {
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = joinPoint.getSignature().getName();
        String target = className + "." + methodName;

        String userId = resolveUserId();
        long start = System.currentTimeMillis();
        try {
            Object result = joinPoint.proceed();
            long elapsed = System.currentTimeMillis() - start;

            if (elapsed >= SLOW_THRESHOLD_MS) {
                log.warn("[{}] {} - slow ({}ms)", userId, target, elapsed);
            } else {
                log.info("[{}] {} - {}ms", userId, target, elapsed);
            }
            return result;
        } catch (BusinessException e) {
            log.warn("[{}] {} - business error: {}", userId, target, e.getErrorCode().name());
            throw e;
        } catch (Exception e) {
            log.error("[{}] {} - unexpected error", userId, target, e);
            throw e;
        }
    }

    private String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId.toString();
        }
        return "anonymous";
    }
}
