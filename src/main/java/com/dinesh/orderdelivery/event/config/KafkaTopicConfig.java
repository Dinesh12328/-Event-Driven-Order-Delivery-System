package com.dinesh.orderdelivery.event.config;

import com.dinesh.orderdelivery.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    private final int partitions;
    private final short replicas;

    public KafkaTopicConfig(
            @Value("${app.events.topic-partitions}") int partitions,
            @Value("${app.events.topic-replicas}") short replicas
    ) {
        this.partitions = partitions;
        this.replicas = replicas;
    }

    @Bean
    NewTopic orderPlacedTopic() {
        return topic(KafkaTopics.ORDER_PLACED);
    }

    @Bean
    NewTopic paymentRequestedTopic() {
        return topic(KafkaTopics.PAYMENT_REQUESTED);
    }

    @Bean
    NewTopic deliveryRequestedTopic() {
        return topic(KafkaTopics.DELIVERY_REQUESTED);
    }

    @Bean
    NewTopic orderStatusChangedTopic() {
        return topic(KafkaTopics.ORDER_STATUS_CHANGED);
    }

    @Bean
    NewTopic notificationEventsTopic() {
        return topic(KafkaTopics.NOTIFICATION_EVENTS);
    }

    private NewTopic topic(String name) {
        TopicBuilder builder = TopicBuilder.name(name).partitions(partitions);
        if (replicas > 0) {
            builder.replicas(replicas);
        }
        return builder.build();
    }
}
