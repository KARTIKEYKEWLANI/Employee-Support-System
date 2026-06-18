package com.support.ticketing.repository;

import com.support.ticketing.entity.SupportQueue;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupportQueueRepository extends JpaRepository<SupportQueue, Long> {
    Optional<SupportQueue> findByNameIgnoreCase(String name);
}
