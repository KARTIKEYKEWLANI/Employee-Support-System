package com.support.ticketing.repository;

import com.support.ticketing.entity.QueueMember;
import com.support.ticketing.entity.SupportQueue;
import com.support.ticketing.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QueueMemberRepository extends JpaRepository<QueueMember, Long> {
    List<QueueMember> findAllByQueue(SupportQueue queue);

    Optional<QueueMember> findByQueueAndUser(SupportQueue queue, User user);

    void deleteAllByUser(User user);
}
