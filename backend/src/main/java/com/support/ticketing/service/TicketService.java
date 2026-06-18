package com.support.ticketing.service;

import com.support.ticketing.dto.ticket.CreateTicketRequest;
import com.support.ticketing.dto.ticket.PagedResponse;
import com.support.ticketing.dto.ticket.TicketAnalyticsResponse;
import com.support.ticketing.dto.ticket.TicketResponse;
import com.support.ticketing.dto.ticket.AssignTicketRequest;
import com.support.ticketing.dto.ticket.AssignTeamRequest;
import com.support.ticketing.dto.ticket.AddCommentRequest;
import com.support.ticketing.dto.ticket.TicketCommentResponse;
import com.support.ticketing.dto.ticket.UpdateQueueRequest;
import com.support.ticketing.dto.ticket.TicketRatingRequest;
import com.support.ticketing.dto.ticket.TicketRatingResponse;
import com.support.ticketing.dto.ticket.UpdateTagsRequest;
import com.support.ticketing.dto.ticket.TicketAttachmentResponse;
import com.support.ticketing.dto.ticket.UpdateTicketStatusRequest;
import com.support.ticketing.security.SecurityUser;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

public interface TicketService {
    TicketResponse createTicket(CreateTicketRequest request, SecurityUser currentUser);

    PagedResponse<TicketResponse> getTickets(SecurityUser currentUser, int page, int size);

    PagedResponse<TicketResponse> getTicketsFiltered(
            SecurityUser currentUser,
            int page,
            int size,
            String query,
            com.support.ticketing.entity.TicketStatus status,
            com.support.ticketing.entity.TicketPriority priority,
            Long queueId,
            Long assigneeId,
            String tag,
            boolean includeArchived
    );

    TicketResponse getTicketById(Long id, SecurityUser currentUser);

    TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request);

    TicketResponse assignTicket(Long id, AssignTicketRequest request);

    TicketResponse assignTeam(Long id, AssignTeamRequest request);

    TicketResponse updateQueue(Long id, UpdateQueueRequest request);

    List<TicketCommentResponse> getComments(Long id, SecurityUser currentUser);

    TicketCommentResponse addComment(Long id, AddCommentRequest request, SecurityUser currentUser);

    TicketRatingResponse addRating(Long id, TicketRatingRequest request, SecurityUser currentUser);

    TicketRatingResponse getRating(Long id, SecurityUser currentUser);

    TicketResponse updateTags(Long id, UpdateTagsRequest request);

    List<String> getAllTags();

    TicketAttachmentResponse addAttachment(Long id, MultipartFile file, SecurityUser currentUser) throws Exception;

    List<TicketAttachmentResponse> getAttachments(Long id, SecurityUser currentUser);

    byte[] exportTickets(SecurityUser currentUser);

    TicketResponse archiveTicket(Long id, SecurityUser currentUser);

    TicketResponse restoreTicket(Long id, SecurityUser currentUser);

    TicketAnalyticsResponse getAnalytics(SecurityUser currentUser);
}
