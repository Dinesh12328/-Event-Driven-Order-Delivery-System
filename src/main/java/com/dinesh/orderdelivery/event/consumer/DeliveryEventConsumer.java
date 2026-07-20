package com.dinesh.orderdelivery.event.consumer;

import com.dinesh.orderdelivery.event.KafkaTopics;
import com.dinesh.orderdelivery.event.domain.IntegrationEventStatus;
import com.dinesh.orderdelivery.event.dto.DeliveryRequestedEvent;
import com.dinesh.orderdelivery.event.service.EventLogService;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class DeliveryEventConsumer {

    private final EventLogService eventLogService;

    public DeliveryEventConsumer(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), dltTopicSuffix = ".DLT")
    @KafkaListener(topics = KafkaTopics.DELIVERY_REQUESTED, groupId = "delivery-service")
    public void onDeliveryRequested(DeliveryRequestedEvent event) {
        eventLogService.record("DeliveryConsumedDeliveryRequestedEvent", event.orderId(), KafkaTopics.DELIVERY_REQUESTED, IntegrationEventStatus.CONSUMED, event, null);
    }

    @DltHandler
    public void onDeadLetter(DeliveryRequestedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        eventLogService.record("DeliveryRequestedDlt", event.orderId(), topic, IntegrationEventStatus.FAILED, event, "Retries exhausted");
    }
}

