package com.support.ticketing.dto.user;

public record UserSummaryResponse(
        Long id,
        String name,
        String email,
        boolean active
) {
}
