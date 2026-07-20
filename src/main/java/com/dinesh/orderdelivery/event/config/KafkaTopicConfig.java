package com.dinesh.orderdelivery.event.config;

import com.dinesh.orderdelivery.event.KafkaTopics;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Bean
    NewTopic orderPlacedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_PLACED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic paymentRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.PAYMENT_REQUESTED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic deliveryRequestedTopic() {
        return TopicBuilder.name(KafkaTopics.DELIVERY_REQUESTED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic orderStatusChangedTopic() {
        return TopicBuilder.name(KafkaTopics.ORDER_STATUS_CHANGED).partitions(3).replicas(1).build();
    }

    @Bean
    NewTopic notificationEventsTopic() {
        return TopicBuilder.name(KafkaTopics.NOTIFICATION_EVENTS).partitions(3).replicas(1).build();
    }
}

