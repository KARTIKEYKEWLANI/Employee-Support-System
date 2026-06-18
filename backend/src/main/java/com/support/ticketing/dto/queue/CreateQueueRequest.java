package com.support.ticketing.dto.queue;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateQueueRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 400) String description,
        boolean autoAssign
) {
}
