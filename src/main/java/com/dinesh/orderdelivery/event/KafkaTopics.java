package com.dinesh.orderdelivery.event;

public final class KafkaTopics {

    public static final String ORDER_PLACED = "order.placed";
    public static final String PAYMENT_REQUESTED = "payment.requested";
    public static final String DELIVERY_REQUESTED = "delivery.requested";
    public static final String ORDER_STATUS_CHANGED = "order.status.changed";
    public static final String NOTIFICATION_EVENTS = "notification.events";

    private KafkaTopics() {
    }
}

