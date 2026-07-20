package com.dinesh.orderdelivery.event.service;

import com.dinesh.orderdelivery.event.domain.IntegrationEventLog;
import com.dinesh.orderdelivery.event.domain.IntegrationEventStatus;
import com.dinesh.orderdelivery.event.dto.IntegrationEventResponse;
import com.dinesh.orderdelivery.event.repository.IntegrationEventLogRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EventLogService {

    private final IntegrationEventLogRepository repository;
    private final ObjectMapper objectMapper;

    public EventLogService(IntegrationEventLogRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public void record(String eventType, UUID aggregateId, String topic, IntegrationEventStatus status, Object payload, String error) {
        repository.save(new IntegrationEventLog(eventType, aggregateId, topic, status, serialize(payload), error));
    }

    @Transactional(readOnly = true)
    public List<IntegrationEventResponse> events() {
        return repository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    private IntegrationEventResponse toResponse(IntegrationEventLog log) {
        return new IntegrationEventResponse(
                log.getId(),
                log.getEventType(),
                log.getAggregateId(),
                log.getTopic(),
                log.getStatus(),
                log.getPayload(),
                log.getErrorMessage(),
                log.getCreatedAt()
        );
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException exception) {
            return "{\"serializationError\":\"" + exception.getMessage() + "\"}";
        }
    }
}

