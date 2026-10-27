package com.careerpilot.security;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {

    private SecurityUtils() {
    }

    /** Anonymous visitors still get an Authentication object, so check for that explicitly. */
    public static boolean isLoggedIn(Authentication authentication) {
        return authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    public static boolean isLoggedIn() {
        return isLoggedIn(SecurityContextHolder.getContext().getAuthentication());
    }
}