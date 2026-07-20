package com.dinesh.orderdelivery.event.consumer;

import com.dinesh.orderdelivery.event.KafkaTopics;
import com.dinesh.orderdelivery.event.domain.IntegrationEventStatus;
import com.dinesh.orderdelivery.event.dto.PaymentRequestedEvent;
import com.dinesh.orderdelivery.event.service.EventLogService;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private final EventLogService eventLogService;

    public PaymentEventConsumer(EventLogService eventLogService) {
        this.eventLogService = eventLogService;
    }

    @RetryableTopic(attempts = "3", backoff = @Backoff(delay = 1000, multiplier = 2.0), dltTopicSuffix = ".DLT")
    @KafkaListener(topics = KafkaTopics.PAYMENT_REQUESTED, groupId = "payment-service")
    public void onPaymentRequested(PaymentRequestedEvent event) {
        eventLogService.record("PaymentConsumedPaymentRequestedEvent", event.orderId(), KafkaTopics.PAYMENT_REQUESTED, IntegrationEventStatus.CONSUMED, event, null);
    }

    @DltHandler
    public void onDeadLetter(PaymentRequestedEvent event, @Header(KafkaHeaders.RECEIVED_TOPIC) String topic) {
        eventLogService.record("PaymentRequestedDlt", event.orderId(), topic, IntegrationEventStatus.FAILED, event, "Retries exhausted");
    }
}

