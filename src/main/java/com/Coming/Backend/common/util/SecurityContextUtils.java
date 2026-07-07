package com.Coming.Backend.common.util;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityContextUtils {

    private SecurityContextUtils() {}

    public static String resolveUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId.toString();
        }
        return "anonymous";
    }
}
