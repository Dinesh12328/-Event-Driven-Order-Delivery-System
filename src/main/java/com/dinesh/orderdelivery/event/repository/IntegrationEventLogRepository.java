package com.dinesh.orderdelivery.event.repository;

import com.dinesh.orderdelivery.event.domain.IntegrationEventLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface IntegrationEventLogRepository extends JpaRepository<IntegrationEventLog, UUID> {
    List<IntegrationEventLog> findAllByOrderByCreatedAtDesc();
}

