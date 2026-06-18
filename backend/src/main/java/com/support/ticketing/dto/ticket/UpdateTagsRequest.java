package com.support.ticketing.dto.ticket;

import java.util.List;

public record UpdateTagsRequest(List<String> tags) {
}
