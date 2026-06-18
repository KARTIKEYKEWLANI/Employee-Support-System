package com.support.ticketing.dto.user;

import jakarta.validation.constraints.NotBlank;

public record UpdateUserRoleRequest(@NotBlank String role) {
}
