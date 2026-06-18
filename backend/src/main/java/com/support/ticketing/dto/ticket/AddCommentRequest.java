package com.support.ticketing.dto.ticket;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AddCommentRequest(
        @NotBlank @Size(max = 2000) String message,
        boolean internalNote
) {
}
