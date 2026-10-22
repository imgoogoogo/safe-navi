package com.safenavi.backend.auth.security;

/** Principal placed in the SecurityContext by {@link JwtAuthenticationFilter}. */
public record AuthUser(Long id, String role) {

    public boolean isAdmin() {
        return "ADMIN".equals(role);
    }
}
