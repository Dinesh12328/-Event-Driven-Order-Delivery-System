package com.dinesh.orderdelivery.event.consumer;

import com.dinesh.orderdelivery.event.DomainEventPublisher;
import com.dinesh.orderdelivery.event.KafkaTopics;
import com.dinesh.orderdelivery.event.domain.IntegrationEventStatus;
import com.dinesh.orderdelivery.event.dto.DeliveryRequestedEvent;
import com.dinesh.orderdelivery.event.dto.NotificationEvent;
import com.dinesh.orderdelivery.event.dto.OrderPlacedEvent;
import com.dinesh.orderdelivery.event.dto.PaymentRequestedEvent;
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
public class RestaurantOrderEventConsumer {

    private final DomainEventPublisher eventPublisher;
    private final EventLogService eventLogService;

    public RestaurantOrderEventConsumer(DomainEventPublisher eventPublisher, EventLogService eventLogService) {
        this.eventPublisher = eventPublisher;
        this.eventLogService = eventLogService;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), dltTopicSuffix = ".DLT")
    @KafkaListener(topics = KafkaTopics.ORDER_PLACED, groupId = "restaurant-service")
    public void onOrderPlaced(OrderPlacedEvent event) {
        eventLogService.record("RestaurantConsumedOrderPlacedEvent", event.orderId(), KafkaTopics.ORDER_PLACED, IntegrationEventStatus.CONSUMED, event, null);
        eventPublisher.publishPaymentRequested(new PaymentRequestedEvent(
                UUID.randomUUID(),
                event.orderId(),
                event.customerId(),
                event.totalPrice(),
                Instant.now()
        ));
        eventPublisher.publishDeliveryRequested(new DeliveryRequestedEvent(
                UUID.randomUUID(),
                event.orderId(),
                event.restaurantId(),
                event.restaurantName(),
                event.deliveryAddress(),
                Instant.now()
        ));
        eventPublisher.publishNotification(new NotificationEvent(
                UUID.randomUUID(),
                event.orderId(),
                "RESTAURANT_OWNER",
                "New order received for " + event.restaurantName(),
                Instant.now()
        ));
    }

    @DltHandler
    public void onDeadLetter(OrderPlacedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        eventLogService.record("RestaurantOrderPlacedDlt", event.orderId(), topic, IntegrationEventStatus.FAILED, event, "Retries exhausted");
    }
}

