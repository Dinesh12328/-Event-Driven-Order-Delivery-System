package com.dinesh.orderdelivery.delivery.domain;

import com.dinesh.orderdelivery.auth.domain.AppUser;
import com.dinesh.orderdelivery.order.domain.CustomerOrder;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "deliveries")
public class Delivery {

    @Id
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    private CustomerOrder customerOrder;

    @ManyToOne(fetch = FetchType.LAZY)
    private AppUser agent;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DeliveryStatus status;

    private Instant estimatedDeliveryTime;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    protected Delivery() {
    }

    public Delivery(CustomerOrder customerOrder) {
        this.customerOrder = customerOrder;
        this.status = DeliveryStatus.WAITING_FOR_AGENT;
    }

    @PrePersist
    void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        Instant now = Instant.now();
        if (createdAt == null) {
            createdAt = now;
        }
        updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public UUID getId() {
        return id;
    }

    public CustomerOrder getCustomerOrder() {
        return customerOrder;
    }

    public AppUser getAgent() {
        return agent;
    }

    public DeliveryStatus getStatus() {
        return status;
    }

    public Instant getEstimatedDeliveryTime() {
        return estimatedDeliveryTime;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assign(AppUser agent, Instant estimatedDeliveryTime) {
        this.agent = agent;
        this.estimatedDeliveryTime = estimatedDeliveryTime;
        this.status = DeliveryStatus.ASSIGNED;
    }

    public void changeStatus(DeliveryStatus status) {
        this.status = status;
    }
}

