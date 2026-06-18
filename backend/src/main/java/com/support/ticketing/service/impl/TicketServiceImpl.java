package com.support.ticketing.service.impl;

import com.support.ticketing.dto.ticket.CreateTicketRequest;
import com.support.ticketing.dto.ticket.PagedResponse;
import com.support.ticketing.dto.ticket.TicketAnalyticsResponse;
import com.support.ticketing.dto.ticket.TicketResponse;
import com.support.ticketing.dto.ticket.AssignTicketRequest;
import com.support.ticketing.dto.ticket.AssignTeamRequest;
import com.support.ticketing.dto.ticket.AddCommentRequest;
import com.support.ticketing.dto.ticket.TicketCommentResponse;
import com.support.ticketing.dto.ticket.UpdateTicketStatusRequest;
import com.support.ticketing.dto.ticket.UpdateQueueRequest;
import com.support.ticketing.dto.ticket.TicketRatingRequest;
import com.support.ticketing.dto.ticket.TicketRatingResponse;
import com.support.ticketing.dto.ticket.UpdateTagsRequest;
import com.support.ticketing.dto.ticket.TicketAttachmentResponse;
import com.support.ticketing.dto.user.UserSummaryResponse;
import com.support.ticketing.entity.CommentType;
import com.support.ticketing.entity.Role;
import com.support.ticketing.entity.Tag;
import com.support.ticketing.entity.Ticket;
import com.support.ticketing.entity.TicketStatus;
import com.support.ticketing.entity.TicketPriority;
import com.support.ticketing.entity.User;
import com.support.ticketing.entity.SupportQueue;
import com.support.ticketing.entity.QueueMember;
import com.support.ticketing.entity.TicketComment;
import com.support.ticketing.entity.TicketRating;
import com.support.ticketing.entity.TicketAttachment;
import com.support.ticketing.exception.BadRequestException;
import com.support.ticketing.exception.ResourceNotFoundException;
import com.support.ticketing.repository.QueueMemberRepository;
import com.support.ticketing.repository.SupportQueueRepository;
import com.support.ticketing.repository.TicketCommentRepository;
import com.support.ticketing.repository.TicketRatingRepository;
import com.support.ticketing.repository.TicketAttachmentRepository;
import com.support.ticketing.repository.TagRepository;
import com.support.ticketing.repository.TicketRepository;
import com.support.ticketing.repository.UserRepository;
import com.support.ticketing.security.SecurityUser;
import com.support.ticketing.service.MailService;
import com.support.ticketing.service.StorageService;
import com.support.ticketing.service.TicketService;
import com.support.ticketing.spec.TicketSpecifications;
import java.util.Optional;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.retry.annotation.Retryable;
import org.springframework.dao.DeadlockLoserDataAccessException;

@Service
@RequiredArgsConstructor
public class TicketServiceImpl implements TicketService {

    private final TicketRepository ticketRepository;
    private final UserRepository userRepository;
    private final SupportQueueRepository supportQueueRepository;
    private final QueueMemberRepository queueMemberRepository;
    private final TicketCommentRepository ticketCommentRepository;
    private final TicketRatingRepository ticketRatingRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final TagRepository tagRepository;
    private final StorageService storageService;
    private final org.springframework.beans.factory.ObjectProvider<MailService> mailServiceProvider;

    @Value("${app.sla.low-hours:72}")
    private long slaLowHours;

    @Value("${app.sla.medium-hours:48}")
    private long slaMediumHours;

    @Value("${app.sla.high-hours:24}")
    private long slaHighHours;

    @Override
    @Transactional
    public TicketResponse createTicket(CreateTicketRequest request, SecurityUser currentUser) {
        User user = getUser(currentUser.getId());
        SupportQueue queue = null;
        if (request.queueId() != null) {
            queue = supportQueueRepository.findById(request.queueId())
                    .orElseThrow(() -> new ResourceNotFoundException("Queue not found."));
        }
        Ticket ticket = ticketRepository.save(Ticket.builder()
                .title(request.title().trim())
                .description(request.description().trim())
                .priority(request.priority())
                .status(TicketStatus.OPEN)
                .user(user)
                .queue(queue)
                .archived(false)
                .dueAt(calculateDueAt(request.priority()))
                .slaBreached(false)
                .build());
        autoAssignIfConfigured(ticket);
        logSystemComment(ticket, "Ticket created.");
        notifyTicketCreated(ticket);
        return toResponse(ticket);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TicketResponse> getTickets(SecurityUser currentUser, int page, int size) {
        return getTicketsFiltered(currentUser, page, size, null, null, null, null, null, null, false);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<TicketResponse> getTicketsFiltered(
            SecurityUser currentUser,
            int page,
            int size,
            String query,
            TicketStatus status,
            TicketPriority priority,
            Long queueId,
            Long assigneeId,
            String tag,
            boolean includeArchived
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean admin = hasAdminRole(currentUser);
        boolean agent = hasAgentRole(currentUser);
        Long effectiveAssignee = assigneeId;
        if (!admin && agent) {
            effectiveAssignee = currentUser.getId();
        }
        boolean restrictToUser = !admin && !agent;
        Page<Ticket> ticketPage = ticketRepository.findAll(
                TicketSpecifications.build(currentUser.getId(), restrictToUser, query, status, priority, queueId, effectiveAssignee, tag, includeArchived),
                pageable
        );

        return new PagedResponse<>(
                ticketPage.getContent().stream().map(this::toResponse).toList(),
                ticketPage.getNumber(),
                ticketPage.getSize(),
                ticketPage.getTotalElements(),
                ticketPage.getTotalPages(),
                ticketPage.isFirst(),
                ticketPage.isLast()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public TicketResponse getTicketById(Long id, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        return toResponse(ticket);
    }

    @Override
    @Transactional
    public TicketResponse updateStatus(Long id, UpdateTicketStatusRequest request) {
        Ticket ticket = getTicket(id);
        validateStatusTransition(ticket.getStatus(), request.status());
        ticket.setStatus(request.status());
        if (request.status() == TicketStatus.RESOLVED) {
            ticket.setResolvedAt(java.time.Instant.now());
            if (ticket.getDueAt() != null && ticket.getResolvedAt().isAfter(ticket.getDueAt())) {
                ticket.setSlaBreached(true);
            }
        }
        Ticket saved = ticketRepository.save(ticket);
        logSystemComment(saved, "Status updated to " + request.status().name().replace("_", " ") + ".");
        notifyStatusUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse assignTicket(Long id, AssignTicketRequest request) {
        Ticket ticket = getTicket(id);
        if (request.assigneeId() == null) {
            ticket.setAssignedTo(null);
            Ticket saved = ticketRepository.save(ticket);
            logSystemComment(saved, "Primary assignee cleared.");
            notifyAssignmentUpdated(saved);
            return toResponse(saved);
        }

        User assignee = getUser(request.assigneeId());
        if (!assignee.isActive()) {
            throw new BadRequestException("Cannot assign deactivated users.");
        }
        if (assignee.getRole() != Role.ROLE_ADMIN && assignee.getRole() != Role.ROLE_AGENT) {
            throw new BadRequestException("Only support staff can be assigned to tickets.");
        }
        ticket.setAssignedTo(assignee);
        ticket.getAssignees().add(assignee);
        Ticket saved = ticketRepository.save(ticket);
        logSystemComment(saved, "Primary assignee set to " + assignee.getName() + ".");
        notifyAssignmentUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse assignTeam(Long id, AssignTeamRequest request) {
        Ticket ticket = getTicket(id);
        Set<User> team = new HashSet<>();
        if (request.assigneeIds() != null) {
            for (Long userId : request.assigneeIds()) {
                User user = getUser(userId);
                if (!user.isActive()) {
                    throw new BadRequestException("Cannot assign deactivated users.");
                }
                if (user.getRole() != Role.ROLE_ADMIN && user.getRole() != Role.ROLE_AGENT) {
                    throw new BadRequestException("Only support staff can be assigned to tickets.");
                }
                team.add(user);
            }
        }
        ticket.setAssignees(team);
        Ticket saved = ticketRepository.save(ticket);
        logSystemComment(saved, "Team assignment updated (" + team.size() + " assignees).");
        notifyAssignmentUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional
    @Retryable(
            retryFor = DeadlockLoserDataAccessException.class,
            maxAttempts = 3
    )
    public TicketResponse updateQueue(Long id, UpdateQueueRequest request) {
        Ticket ticket = getTicket(id);
        if (request.queueId() == null) {
            ticket.setQueue(null);
            Ticket saved = ticketRepository.save(ticket);
            logSystemComment(saved, "Queue cleared.");
            return toResponse(saved);
        }
        SupportQueue queue = supportQueueRepository.findById(request.queueId())
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found."));
        ticket.setQueue(queue);
        Ticket saved = ticketRepository.save(ticket);
        logSystemComment(saved, "Queue set to " + queue.getName() + ".");
        autoAssignIfConfigured(saved);
        notifyAssignmentUpdated(saved);
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketCommentResponse> getComments(Long id, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        return ticketCommentRepository.findAllByTicketOrderByCreatedAtAsc(ticket).stream()
                .filter(comment -> hasAdminRole(currentUser) || !comment.isInternalNote())
                .map(this::toCommentResponse)
                .toList();
    }

    @Override
    @Transactional
    public TicketCommentResponse addComment(Long id, AddCommentRequest request, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        boolean internal = request.internalNote() && (hasAdminRole(currentUser) || hasAgentRole(currentUser));
        TicketComment comment = ticketCommentRepository.save(TicketComment.builder()
                .ticket(ticket)
                .author(getUser(currentUser.getId()))
                .message(request.message().trim())
                .type(CommentType.USER)
                .internalNote(internal)
                .build());
        notifyCommentAdded(ticket, comment);
        return toCommentResponse(comment);
    }

    @Override
    @Transactional
    public TicketRatingResponse addRating(Long id, TicketRatingRequest request, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        if (ticket.getStatus() != TicketStatus.RESOLVED) {
            throw new BadRequestException("Ticket must be resolved before rating.");
        }
        if (ticketRatingRepository.findByTicket(ticket).isPresent()) {
            throw new BadRequestException("Ticket already rated.");
        }
        TicketRating rating = ticketRatingRepository.save(TicketRating.builder()
                .ticket(ticket)
                .user(getUser(currentUser.getId()))
                .score(request.score())
                .feedback(request.feedback() == null ? null : request.feedback().trim())
                .build());
        logSystemComment(ticket, "Customer rating submitted.");
        notifyRatingSubmitted(ticket, rating);
        return toRatingResponse(rating);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketRatingResponse getRating(Long id, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        return ticketRatingRepository.findByTicket(ticket)
                .map(this::toRatingResponse)
                .orElse(null);
    }

    @Override
    @Transactional
    public TicketResponse updateTags(Long id, UpdateTagsRequest request) {
        Ticket ticket = getTicket(id);
        Set<Tag> tags = new HashSet<>();
        if (request.tags() != null) {
            for (String raw : request.tags()) {
                if (raw == null || raw.isBlank()) {
                    continue;
                }
                String name = raw.trim().toLowerCase();
                Tag tag = tagRepository.findByNameIgnoreCase(name)
                        .orElseGet(() -> tagRepository.save(Tag.builder().name(name).build()));
                tags.add(tag);
            }
        }
        ticket.setTags(tags);
        Ticket saved = ticketRepository.save(ticket);
        logSystemComment(saved, "Tags updated.");
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getAllTags() {
        return tagRepository.findAll().stream().map(Tag::getName).toList();
    }

    @Override
    @Transactional
    public TicketAttachmentResponse addAttachment(Long id, MultipartFile file, SecurityUser currentUser) throws Exception {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        String storagePath = storageService.save(file.getOriginalFilename(), file.getInputStream());
        TicketAttachment attachment = ticketAttachmentRepository.save(TicketAttachment.builder()
                .ticket(ticket)
                .uploadedBy(getUser(currentUser.getId()))
                .filename(file.getOriginalFilename() == null ? "file" : file.getOriginalFilename())
                .contentType(file.getContentType() == null ? "application/octet-stream" : file.getContentType())
                .size(file.getSize())
                .storagePath(storagePath)
                .build());
        logSystemComment(ticket, "Attachment added: " + attachment.getFilename());
        return toAttachmentResponse(attachment);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TicketAttachmentResponse> getAttachments(Long id, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        return ticketAttachmentRepository.findAllByTicket(ticket).stream()
                .map(this::toAttachmentResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportTickets(SecurityUser currentUser) {
        boolean admin = hasAdminRole(currentUser);
        List<Ticket> tickets = admin ? ticketRepository.findAll() : ticketRepository.findAllByUser(getUser(currentUser.getId()));
        String header = "id,title,priority,status,queue,assignees,tags,createdAt,updatedAt,resolvedAt,dueAt,slaBreached\n";
        String body = tickets.stream()
                .map(ticket -> String.join(",",
                        String.valueOf(ticket.getId()),
                        escape(ticket.getTitle()),
                        String.valueOf(ticket.getPriority()),
                        String.valueOf(ticket.getStatus()),
                        ticket.getQueue() != null ? escape(ticket.getQueue().getName()) : "",
                        escape(ticket.getAssignees().stream().map(User::getName).collect(Collectors.joining("|"))),
                        escape(ticket.getTags().stream().map(Tag::getName).collect(Collectors.joining("|"))),
                        String.valueOf(ticket.getCreatedAt()),
                        String.valueOf(ticket.getUpdatedAt()),
                        String.valueOf(ticket.getResolvedAt()),
                        String.valueOf(ticket.getDueAt()),
                        String.valueOf(ticket.isSlaBreached())
                ))
                .collect(Collectors.joining("\n"));
        return (header + body).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    @Override
    @Transactional
    public TicketResponse archiveTicket(Long id, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        ticket.setArchived(true);
        Ticket saved = ticketRepository.save(ticket);
        logSystemComment(saved, "Ticket archived.");
        return toResponse(saved);
    }

    @Override
    @Transactional
    public TicketResponse restoreTicket(Long id, SecurityUser currentUser) {
        Ticket ticket = getTicket(id);
        if (!hasAdminRole(currentUser) && !ticket.getUser().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("Ticket not found.");
        }
        ticket.setArchived(false);
        Ticket saved = ticketRepository.save(ticket);
        logSystemComment(saved, "Ticket restored.");
        return toResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TicketAnalyticsResponse getAnalytics(SecurityUser currentUser) {
        List<Ticket> tickets;
        if (hasAdminRole(currentUser)) {
            tickets = ticketRepository.findAll();
        } else if (hasAgentRole(currentUser)) {
            tickets = ticketRepository.findAll(
                    TicketSpecifications.build(currentUser.getId(), false, null, null, null, null, currentUser.getId(), null, true)
            );
        } else {
            tickets = ticketRepository.findAllByUser(getUser(currentUser.getId()));
        }

        long open = tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.OPEN).count();
        long inProgress = tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.IN_PROGRESS).count();
        long resolved = tickets.stream().filter(ticket -> ticket.getStatus() == TicketStatus.RESOLVED).count();

        return new TicketAnalyticsResponse(tickets.size(), open, inProgress, resolved);
    }

    private void validateStatusTransition(TicketStatus currentStatus, TicketStatus newStatus) {
        if (currentStatus == newStatus) {
            return;
        }

        if (currentStatus == TicketStatus.OPEN && newStatus == TicketStatus.IN_PROGRESS) {
            return;
        }

        if (currentStatus == TicketStatus.IN_PROGRESS && newStatus == TicketStatus.RESOLVED) {
            return;
        }

        throw new BadRequestException("Invalid status transition from " + currentStatus + " to " + newStatus + ".");
    }

    private Ticket getTicket(Long id) {
        return ticketRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found."));
    }

    private User getUser(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private boolean hasAdminRole(SecurityUser currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(Role.ROLE_ADMIN.name()));
    }

    private boolean hasAgentRole(SecurityUser currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals(Role.ROLE_AGENT.name()));
    }

    private TicketResponse toResponse(Ticket ticket) {
        User assignee = ticket.getAssignedTo();
        List<UserSummaryResponse> assignees = ticket.getAssignees().stream()
                .map(user -> new UserSummaryResponse(user.getId(), user.getName(), user.getEmail(), user.isActive()))
                .toList();
        return new TicketResponse(
                ticket.getId(),
                ticket.getTitle(),
                ticket.getDescription(),
                ticket.getPriority(),
                ticket.getStatus(),
                ticket.getUser().getId(),
                ticket.getUser().getName(),
                ticket.getUser().getEmail(),
                assignee != null ? assignee.getId() : null,
                assignee != null ? assignee.getName() : null,
                assignee != null ? assignee.getEmail() : null,
                assignees,
                ticket.getQueue() != null ? ticket.getQueue().getId() : null,
                ticket.getQueue() != null ? ticket.getQueue().getName() : null,
                ticket.getTags().stream().map(Tag::getName).toList(),
                ticket.getDueAt(),
                ticket.isSlaBreached(),
                ticket.getResolvedAt(),
                ticket.isArchived(),
                ticket.getCreatedAt(),
                ticket.getUpdatedAt()
        );
    }

    private void autoAssignIfConfigured(Ticket ticket) {
        SupportQueue queue = ticket.getQueue();
        if (queue == null || !queue.isAutoAssign()) {
            return;
        }

        List<User> candidates = new ArrayList<>();
        List<QueueMember> members = queueMemberRepository.findAllByQueue(queue);
        for (QueueMember member : members) {
            if (member.getUser().isActive()) {
                candidates.add(member.getUser());
            }
        }
        if (candidates.isEmpty()) {
            candidates = userRepository.findAllByRoleIn(List.of(Role.ROLE_ADMIN, Role.ROLE_AGENT)).stream()
                    .filter(User::isActive)
                    .toList();
        }

        if (candidates.isEmpty()) {
            return;
        }

        Collection<TicketStatus> active = List.of(TicketStatus.OPEN, TicketStatus.IN_PROGRESS);
        User selected = null;
        long bestCount = Long.MAX_VALUE;
        for (User candidate : candidates) {
            long count = queue != null
                    ? ticketRepository.countByAssigneesContainingAndQueueAndStatusIn(candidate, queue, active)
                    : ticketRepository.countByAssigneesContainingAndStatusIn(candidate, active);
            if (count < bestCount) {
                bestCount = count;
                selected = candidate;
            }
        }

        if (selected != null) {
            ticket.getAssignees().clear();
            ticket.getAssignees().add(selected);
            ticket.setAssignedTo(selected);
            ticketRepository.save(ticket);
        }
    }

    private void logSystemComment(Ticket ticket, String message) {
        ticketCommentRepository.save(TicketComment.builder()
                .ticket(ticket)
                .author(ticket.getUser())
                .message(message)
                .type(CommentType.SYSTEM)
                .internalNote(false)
                .build());
    }

    private TicketCommentResponse toCommentResponse(TicketComment comment) {
        return new TicketCommentResponse(
                comment.getId(),
                comment.getAuthor().getId(),
                comment.getAuthor().getName(),
                comment.getMessage(),
                comment.getType(),
                comment.isInternalNote(),
                comment.getCreatedAt()
        );
    }

    private TicketRatingResponse toRatingResponse(TicketRating rating) {
        return new TicketRatingResponse(
                rating.getId(),
                rating.getScore(),
                rating.getFeedback(),
                rating.getCreatedAt()
        );
    }

    private java.time.Instant calculateDueAt(TicketPriority priority) {
        long hours = switch (priority) {
            case HIGH -> slaHighHours;
            case MEDIUM -> slaMediumHours;
            case LOW -> slaLowHours;
        };
        return java.time.Instant.now().plusSeconds(hours * 3600);
    }

    private TicketAttachmentResponse toAttachmentResponse(TicketAttachment attachment) {
        return new TicketAttachmentResponse(
                attachment.getId(),
                attachment.getFilename(),
                attachment.getContentType(),
                attachment.getSize(),
                attachment.getCreatedAt()
        );
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        String escaped = value.replace("\"", "\"\"");
        if (escaped.contains(",") || escaped.contains("\"") || escaped.contains("\n")) {
            return "\"" + escaped + "\"";
        }
        return escaped;
    }

    private Optional<MailService> mailService() {
        return Optional.ofNullable(mailServiceProvider.getIfAvailable());
    }

    private void notifyTicketCreated(Ticket ticket) {
        mailService().ifPresent(service -> service.sendEmail(
                ticket.getUser().getEmail(),
                "Ticket Created: " + ticket.getTitle(),
                "Your ticket has been created and is now OPEN."
        ));
    }

    private void notifyStatusUpdated(Ticket ticket) {
        mailService().ifPresent(service -> service.sendEmail(
                ticket.getUser().getEmail(),
                "Ticket Status Update: " + ticket.getTitle(),
                "Your ticket status is now " + ticket.getStatus().name().replace("_", " ") + "."
        ));
    }

    private void notifyAssignmentUpdated(Ticket ticket) {
        mailService().ifPresent(service -> service.sendEmail(
                ticket.getUser().getEmail(),
                "Ticket Assignment Updated",
                "Your ticket has been assigned to support."
        ));
    }

    private void notifyCommentAdded(Ticket ticket, TicketComment comment) {
        if (comment.isInternalNote()) {
            return;
        }
        mailService().ifPresent(service -> service.sendEmail(
                ticket.getUser().getEmail(),
                "New update on your ticket",
                "A new comment was added to your ticket."
        ));
    }

    private void notifyRatingSubmitted(Ticket ticket, TicketRating rating) {
        mailService().ifPresent(service -> service.sendEmail(
                ticket.getUser().getEmail(),
                "Thanks for your feedback",
                "We received your rating: " + rating.getScore() + "/5."
        ));
    }
}
