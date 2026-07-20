package com.dinesh.orderdelivery.event.dto;

import com.dinesh.orderdelivery.event.domain.IntegrationEventStatus;
import java.time.Instant;
import java.util.UUID;

public record IntegrationEventResponse(
        UUID id,
        String eventType,
        UUID aggregateId,
        String topic,
        IntegrationEventStatus status,
        String payload,
        String errorMessage,
        Instant createdAt
) {
}

