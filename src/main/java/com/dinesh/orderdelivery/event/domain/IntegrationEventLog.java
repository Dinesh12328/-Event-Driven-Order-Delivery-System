package com.dinesh.orderdelivery.event.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "integration_event_logs")
public class IntegrationEventLog {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false)
    private UUID aggregateId;

    @Column(nullable = false)
    private String topic;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private IntegrationEventStatus status;

    @Column(nullable = false, length = 8000)
    private String payload;

    @Column(length = 2000)
    private String errorMessage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected IntegrationEventLog() {
    }

    public IntegrationEventLog(
            String eventType,
            UUID aggregateId,
            String topic,
            IntegrationEventStatus status,
            String payload,
            String errorMessage
    ) {
        this.eventType = eventType;
        this.aggregateId = aggregateId;
        this.topic = topic;
        this.status = status;
        this.payload = payload;
        this.errorMessage = errorMessage;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public UUID getId() {
        return id;
    }

    public String getEventType() {
        return eventType;
    }

    public UUID getAggregateId() {
        return aggregateId;
    }

    public String getTopic() {
        return topic;
    }

    public IntegrationEventStatus getStatus() {
        return status;
    }

    public String getPayload() {
        return payload;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}

