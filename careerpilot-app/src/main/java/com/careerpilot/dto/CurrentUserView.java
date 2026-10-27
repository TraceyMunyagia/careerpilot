package com.careerpilot.dto;

import com.careerpilot.security.CareerPilotUserDetails;
import com.careerpilot.security.SecurityUtils;
import org.springframework.security.core.Authentication;

/** The bits of the logged-in user that pages display. null means nobody is logged in. */
public record CurrentUserView(String fullName, String email) {

    public String firstName() {
        int space = fullName.indexOf(' ');
        return space > 0 ? fullName.substring(0, space) : fullName;
    }

    public static CurrentUserView from(Authentication authentication) {
        if (!SecurityUtils.isLoggedIn(authentication)) {
            return null;
        }
        if (authentication.getPrincipal() instanceof CareerPilotUserDetails details) {
            return new CurrentUserView(details.getFullName(), details.getUsername());
        }
        // e.g. test users created with @WithMockUser
        return new CurrentUserView(authentication.getName(), authentication.getName());
    }
}