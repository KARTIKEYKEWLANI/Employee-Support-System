package com.support.ticketing.dto.auth;

public record AuthResponse(
        String token,
        Long id,
        String name,
        String email,
        String role
) {
}
