package com.support.ticketing.controller;

import com.support.ticketing.dto.ticket.CreateTicketRequest;
import com.support.ticketing.dto.ticket.PagedResponse;
import com.support.ticketing.dto.ticket.TicketAnalyticsResponse;
import com.support.ticketing.dto.ticket.TicketResponse;
import com.support.ticketing.dto.ticket.AssignTicketRequest;
import com.support.ticketing.dto.ticket.AssignTeamRequest;
import com.support.ticketing.dto.ticket.AddCommentRequest;
import com.support.ticketing.dto.ticket.TicketCommentResponse;
import com.support.ticketing.dto.ticket.TicketRatingRequest;
import com.support.ticketing.dto.ticket.TicketRatingResponse;
import com.support.ticketing.dto.ticket.UpdateTagsRequest;
import com.support.ticketing.dto.ticket.TicketAttachmentResponse;
import com.support.ticketing.dto.ticket.UpdateTicketStatusRequest;
import com.support.ticketing.dto.ticket.UpdateQueueRequest;
import com.support.ticketing.security.SecurityUser;
import com.support.ticketing.service.TicketService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/tickets")
@RequiredArgsConstructor
public class TicketController {

    private final TicketService ticketService;

    @PostMapping
    public TicketResponse createTicket(
            @Valid @RequestBody CreateTicketRequest request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.createTicket(request, currentUser);
    }

    @GetMapping
    public PagedResponse<TicketResponse> getTickets(
            @AuthenticationPrincipal SecurityUser currentUser,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) com.support.ticketing.entity.TicketStatus status,
            @RequestParam(required = false) com.support.ticketing.entity.TicketPriority priority,
            @RequestParam(required = false) Long queueId,
            @RequestParam(required = false) Long assigneeId,
            @RequestParam(required = false) String tag,
            @RequestParam(defaultValue = "false") boolean includeArchived
    ) {
        return ticketService.getTicketsFiltered(currentUser, page, size, query, status, priority, queueId, assigneeId, tag, includeArchived);
    }

    @GetMapping("/{id}")
    public TicketResponse getTicketById(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.getTicketById(id, currentUser);
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','AGENT')")
    public TicketResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTicketStatusRequest request
    ) {
        return ticketService.updateStatus(id, request);
    }

    @PutMapping("/{id}/assign")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse assignTicket(
            @PathVariable Long id,
            @Valid @RequestBody AssignTicketRequest request
    ) {
        return ticketService.assignTicket(id, request);
    }

    @PutMapping("/{id}/team")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse assignTeam(
            @PathVariable Long id,
            @Valid @RequestBody AssignTeamRequest request
    ) {
        return ticketService.assignTeam(id, request);
    }

    @PutMapping("/{id}/queue")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse updateQueue(
            @PathVariable Long id,
            @RequestBody UpdateQueueRequest request
    ) {
        return ticketService.updateQueue(id, request);
    }

    @GetMapping("/analytics")
    public TicketAnalyticsResponse getAnalytics(@AuthenticationPrincipal SecurityUser currentUser) {
        return ticketService.getAnalytics(currentUser);
    }

    @GetMapping("/{id}/comments")
    public List<TicketCommentResponse> getComments(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.getComments(id, currentUser);
    }

    @PostMapping("/{id}/comments")
    public TicketCommentResponse addComment(
            @PathVariable Long id,
            @Valid @RequestBody AddCommentRequest request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.addComment(id, request, currentUser);
    }

    @GetMapping("/{id}/rating")
    public TicketRatingResponse getRating(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.getRating(id, currentUser);
    }

    @PostMapping("/{id}/rating")
    public TicketRatingResponse addRating(
            @PathVariable Long id,
            @Valid @RequestBody TicketRatingRequest request,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.addRating(id, request, currentUser);
    }

    @PutMapping("/{id}/tags")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse updateTags(@PathVariable Long id, @RequestBody UpdateTagsRequest request) {
        return ticketService.updateTags(id, request);
    }

    @GetMapping("/tags")
    public List<String> getTags() {
        return ticketService.getAllTags();
    }

    @PostMapping(value = "/{id}/attachments", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public TicketAttachmentResponse uploadAttachment(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal SecurityUser currentUser
    ) throws Exception {
        return ticketService.addAttachment(id, file, currentUser);
    }

    @GetMapping("/{id}/attachments")
    public List<TicketAttachmentResponse> getAttachments(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.getAttachments(id, currentUser);
    }

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> exportTickets(@AuthenticationPrincipal SecurityUser currentUser) {
        byte[] data = ticketService.exportTickets(currentUser);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=tickets.csv")
                .contentType(MediaType.valueOf("text/csv"))
                .body(data);
    }

    @PutMapping("/{id}/archive")
    public TicketResponse archiveTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.archiveTicket(id, currentUser);
    }

    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public TicketResponse restoreTicket(
            @PathVariable Long id,
            @AuthenticationPrincipal SecurityUser currentUser
    ) {
        return ticketService.restoreTicket(id, currentUser);
    }
}
