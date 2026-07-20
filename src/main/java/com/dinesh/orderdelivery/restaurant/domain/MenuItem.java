package com.dinesh.orderdelivery.restaurant.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "menu_items")
public class MenuItem {

    @Id
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Restaurant restaurant;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 1000)
    private String description;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private boolean available;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    protected MenuItem() {
    }

    public MenuItem(Restaurant restaurant, String name, String description, BigDecimal price, boolean available) {
        this.restaurant = restaurant;
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
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

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public boolean isAvailable() {
        return available;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void update(String name, String description, BigDecimal price, boolean available) {
        this.name = name;
        this.description = description;
        this.price = price;
        this.available = available;
    }
}

