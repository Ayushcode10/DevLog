package com.devlog.devlog_backend.util;

import com.devlog.devlog_backend.entity.User;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class SecurityUtils {

    // WHY this util: after JwtAuthFilter runs, it stores the authenticated
    // User in the SecurityContext. This method retrieves it.
    // We call this at the START of every service method so we always
    // know which user owns the data being operated on.
    public User getCurrentUser() {
        return (User) SecurityContextHolder
                .getContext()
                .getAuthentication()
                .getPrincipal();
    }
}