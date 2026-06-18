package com.support.ticketing.dto.queue;

public record QueueMemberResponse(
        Long id,
        Long userId,
        String name,
        String email
) {
}
