package com.support.ticketing.service.impl;

import com.support.ticketing.dto.queue.CreateQueueRequest;
import com.support.ticketing.dto.queue.QueueMemberResponse;
import com.support.ticketing.dto.queue.QueueResponse;
import com.support.ticketing.entity.QueueMember;
import com.support.ticketing.entity.Role;
import com.support.ticketing.entity.SupportQueue;
import com.support.ticketing.entity.User;
import com.support.ticketing.exception.BadRequestException;
import com.support.ticketing.exception.ResourceNotFoundException;
import com.support.ticketing.repository.QueueMemberRepository;
import com.support.ticketing.repository.SupportQueueRepository;
import com.support.ticketing.repository.UserRepository;
import com.support.ticketing.service.QueueService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class QueueServiceImpl implements QueueService {

    private final SupportQueueRepository supportQueueRepository;
    private final QueueMemberRepository queueMemberRepository;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public QueueResponse createQueue(CreateQueueRequest request) {
        if (supportQueueRepository.findByNameIgnoreCase(request.name().trim()).isPresent()) {
            throw new BadRequestException("Queue name already exists.");
        }
        SupportQueue queue = supportQueueRepository.save(SupportQueue.builder()
                .name(request.name().trim())
                .description(request.description() == null ? null : request.description().trim())
                .autoAssign(request.autoAssign())
                .build());
        return toResponse(queue);
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueResponse> getQueues() {
        return supportQueueRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<QueueMemberResponse> getMembers(Long queueId) {
        SupportQueue queue = getQueue(queueId);
        return queueMemberRepository.findAllByQueue(queue).stream()
                .map(member -> new QueueMemberResponse(member.getId(), member.getUser().getId(), member.getUser().getName(), member.getUser().getEmail()))
                .toList();
    }

    @Override
    @Transactional
    public QueueMemberResponse addMember(Long queueId, Long userId) {
        SupportQueue queue = getQueue(queueId);
        User user = getUser(userId);
        if (user.getRole() != Role.ROLE_ADMIN && user.getRole() != Role.ROLE_AGENT) {
            throw new BadRequestException("Only support staff can be added to queues.");
        }
        if (!user.isActive()) {
            throw new BadRequestException("Cannot add deactivated users to queues.");
        }
        if (queueMemberRepository.findByQueueAndUser(queue, user).isPresent()) {
            throw new BadRequestException("User already in this queue.");
        }
        QueueMember member = queueMemberRepository.save(QueueMember.builder()
                .queue(queue)
                .user(user)
                .build());
        return new QueueMemberResponse(member.getId(), user.getId(), user.getName(), user.getEmail());
    }

    @Override
    @Transactional
    public void removeMember(Long queueId, Long userId) {
        SupportQueue queue = getQueue(queueId);
        User user = getUser(userId);
        QueueMember member = queueMemberRepository.findByQueueAndUser(queue, user)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found."));
        queueMemberRepository.delete(member);
    }

    private SupportQueue getQueue(Long queueId) {
        return supportQueueRepository.findById(queueId)
                .orElseThrow(() -> new ResourceNotFoundException("Queue not found."));
    }

    private User getUser(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));
    }

    private QueueResponse toResponse(SupportQueue queue) {
        return new QueueResponse(queue.getId(), queue.getName(), queue.getDescription(), queue.isAutoAssign(), queue.getCreatedAt());
    }
}
