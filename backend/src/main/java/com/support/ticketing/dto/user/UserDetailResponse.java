package com.support.ticketing.dto.user;

public record UserDetailResponse(
        Long id,
        String name,
        String email,
        String role,
        boolean active
){
}
