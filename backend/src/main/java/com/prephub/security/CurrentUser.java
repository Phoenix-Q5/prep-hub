package com.prephub.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class CurrentUser {

    public Optional<AuthenticatedUser> get() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof AuthenticatedUser u) {
            return Optional.of(u);
        }
        return Optional.empty();
    }

    public AuthenticatedUser getRequired() {
        return get().orElseThrow(() -> new IllegalStateException("No authenticated user"));
    }
}
