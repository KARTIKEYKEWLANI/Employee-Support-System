package com.support.ticketing.dto.ticket;

import java.util.List;

public record AssignTeamRequest(List<Long> assigneeIds) {
}
