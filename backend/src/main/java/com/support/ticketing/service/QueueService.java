package com.support.ticketing.service;

import com.support.ticketing.dto.queue.CreateQueueRequest;
import com.support.ticketing.dto.queue.QueueMemberResponse;
import com.support.ticketing.dto.queue.QueueResponse;
import java.util.List;

public interface QueueService {
    QueueResponse createQueue(CreateQueueRequest request);

    List<QueueResponse> getQueues();

    List<QueueMemberResponse> getMembers(Long queueId);

    QueueMemberResponse addMember(Long queueId, Long userId);

    void removeMember(Long queueId, Long userId);
}
