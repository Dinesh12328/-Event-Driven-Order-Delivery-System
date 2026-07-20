package com.dinesh.orderdelivery.event;

import com.dinesh.orderdelivery.event.domain.IntegrationEventStatus;
import com.dinesh.orderdelivery.event.dto.DeliveryRequestedEvent;
import com.dinesh.orderdelivery.event.dto.NotificationEvent;
import com.dinesh.orderdelivery.event.dto.OrderPlacedEvent;
import com.dinesh.orderdelivery.event.dto.OrderStatusChangedEvent;
import com.dinesh.orderdelivery.event.dto.PaymentRequestedEvent;
import com.dinesh.orderdelivery.event.service.EventLogService;
import com.dinesh.orderdelivery.order.domain.CustomerOrder;
import com.dinesh.orderdelivery.order.domain.OrderStatus;
import java.time.Instant;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class DomainEventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final EventLogService eventLogService;
    private final boolean kafkaEnabled;

    public DomainEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            EventLogService eventLogService,
            @Value("${app.events.kafka-enabled}") boolean kafkaEnabled
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.eventLogService = eventLogService;
        this.kafkaEnabled = kafkaEnabled;
    }

    public void publishOrderPlaced(CustomerOrder order) {
        OrderPlacedEvent event = new OrderPlacedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getCustomer().getId(),
                order.getRestaurant().getId(),
                order.getRestaurant().getName(),
                order.getDeliveryAddress(),
                order.getTotalPrice(),
                order.getStatus(),
                Instant.now()
        );
        publish(KafkaTopics.ORDER_PLACED, order.getId().toString(), event);
    }

    public void publishOrderStatusChanged(CustomerOrder order, OrderStatus previousStatus, OrderStatus nextStatus) {
        OrderStatusChangedEvent event = new OrderStatusChangedEvent(
                UUID.randomUUID(),
                order.getId(),
                order.getRestaurant().getId(),
                previousStatus,
                nextStatus,
                Instant.now()
        );
        publish(KafkaTopics.ORDER_STATUS_CHANGED, order.getId().toString(), event);
    }

    public void publishPaymentRequested(PaymentRequestedEvent event) {
        publish(KafkaTopics.PAYMENT_REQUESTED, event.orderId().toString(), event);
    }

    public void publishDeliveryRequested(DeliveryRequestedEvent event) {
        publish(KafkaTopics.DELIVERY_REQUESTED, event.orderId().toString(), event);
    }

    public void publishNotification(NotificationEvent event) {
        publish(KafkaTopics.NOTIFICATION_EVENTS, event.orderId().toString(), event);
    }

    private void publish(String topic, String key, Object event) {
        UUID aggregateId = aggregateId(event);
        if (!kafkaEnabled) {
            eventLogService.record(event.getClass().getSimpleName(), aggregateId, topic, IntegrationEventStatus.LOCAL, event, null);
            return;
        }
        kafkaTemplate.send(topic, key, event)
                .whenComplete((result, exception) -> {
                    if (exception == null) {
                        eventLogService.record(event.getClass().getSimpleName(), aggregateId, topic, IntegrationEventStatus.PUBLISHED, event, null);
                    } else {
                        eventLogService.record(event.getClass().getSimpleName(), aggregateId, topic, IntegrationEventStatus.FAILED, event, exception.getMessage());
                    }
                });
    }

    private UUID aggregateId(Object event) {
        if (event instanceof OrderPlacedEvent orderPlacedEvent) {
            return orderPlacedEvent.orderId();
        }
        if (event instanceof OrderStatusChangedEvent statusChangedEvent) {
            return statusChangedEvent.orderId();
        }
        if (event instanceof PaymentRequestedEvent paymentRequestedEvent) {
            return paymentRequestedEvent.orderId();
        }
        if (event instanceof DeliveryRequestedEvent deliveryRequestedEvent) {
            return deliveryRequestedEvent.orderId();
        }
        if (event instanceof NotificationEvent notificationEvent) {
            return notificationEvent.orderId();
        }
        return UUID.randomUUID();
    }
}

