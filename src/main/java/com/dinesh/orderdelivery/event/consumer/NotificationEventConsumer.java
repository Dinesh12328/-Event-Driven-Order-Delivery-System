package com.dinesh.orderdelivery.event.consumer;

import com.dinesh.orderdelivery.event.KafkaTopics;
import com.dinesh.orderdelivery.event.domain.IntegrationEventStatus;
import com.dinesh.orderdelivery.event.dto.NotificationEvent;
import com.dinesh.orderdelivery.event.dto.OrderStatusChangedEvent;
import com.dinesh.orderdelivery.event.service.EventLogService;
import java.time.Instant;
import java.util.UUID;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class NotificationEventConsumer {

    private final EventLogService eventLogService;

    public NotificationEventConsumer(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), dltTopicSuffix = ".DLT")
    @KafkaListener(topics = KafkaTopics.ORDER_STATUS_CHANGED, groupId = "notification-service")
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        NotificationEvent notification = new NotificationEvent(
                UUID.randomUUID(),
                event.orderId(),
                "CUSTOMER",
                "Order status changed to " + event.newStatus(),
                Instant.now()
        );
        eventLogService.record("NotificationConsumedOrderStatusChangedEvent", event.orderId(), KafkaTopics.ORDER_STATUS_CHANGED, IntegrationEventStatus.CONSUMED, notification, null);
    }

    @KafkaListener(topics = KafkaTopics.NOTIFICATION_EVENTS, groupId = "notification-service")
    public void onNotification(NotificationEvent event) {
        eventLogService.record("NotificationConsumedNotificationEvent", event.orderId(), KafkaTopics.NOTIFICATION_EVENTS, IntegrationEventStatus.CONSUMED, event, null);
    }

    @DltHandler
    public void onDeadLetter(OrderStatusChangedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        eventLogService.record("OrderStatusChangedDlt", event.orderId(), topic, IntegrationEventStatus.FAILED, event, "Retries exhausted");
    }
}

