package com.support.ticketing.dto.ticket;

import com.support.ticketing.entity.TicketPriority;
import com.support.ticketing.entity.TicketStatus;
import com.support.ticketing.dto.user.UserSummaryResponse;
import java.time.Instant;
import java.util.List;

public record TicketResponse(
        Long id,
        String title,
        String description,
        TicketPriority priority,
        TicketStatus status,
        Long userId,
        String userName,
        String userEmail,
        Long assignedToId,
        String assignedToName,
        String assignedToEmail,
        List<UserSummaryResponse> assignees,
        Long queueId,
        String queueName,
        List<String> tags,
        Instant dueAt,
        boolean slaBreached,
        Instant resolvedAt,
        boolean archived,
        Instant createdAt,
        Instant updatedAt
) {
}
