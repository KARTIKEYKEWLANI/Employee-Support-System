package com.support.ticketing.controller;

import com.support.ticketing.dto.queue.CreateQueueRequest;
import com.support.ticketing.dto.queue.QueueMemberResponse;
import com.support.ticketing.dto.queue.QueueResponse;
import com.support.ticketing.service.QueueService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/queues")
@RequiredArgsConstructor
public class QueueController {

    private final QueueService queueService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public QueueResponse createQueue(@Valid @RequestBody CreateQueueRequest request) {
        return queueService.createQueue(request);
    }

    @GetMapping
    public List<QueueResponse> getQueues() {
        return queueService.getQueues();
    }

    @GetMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public List<QueueMemberResponse> getMembers(@PathVariable Long id) {
        return queueService.getMembers(id);
    }

    @PostMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public QueueMemberResponse addMember(@PathVariable Long id, @PathVariable Long userId) {
        return queueService.addMember(id, userId);
    }

    @DeleteMapping("/{id}/members/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public void removeMember(@PathVariable Long id, @PathVariable Long userId) {
        queueService.removeMember(id, userId);
    }
}
