# Event-Driven Order Delivery System

Mini Swiggy/Zomato-style backend focused on backend engineering: modular Spring Boot services, JWT security, PostgreSQL persistence, Kafka event flow, Redis caching, Docker setup, endpoint tests, and an immersive admin dashboard.

## Sprint Plan

1. **Project setup and base architecture**: Spring Boot, layered packages, PostgreSQL config, Docker Compose, Swagger, validation, global exceptions, health check.
2. **Authentication and user management**: register/login, JWT, role-based APIs, password hashing.
3. **Restaurant and menu system**: owner CRUD, menu management, search/filter, Redis cache hooks.
4. **Order lifecycle**: order creation, item persistence, total calculation, protected status transitions.
5. **Kafka event flow**: order/payment/delivery/notification events with retryable listeners.
6. **Payment and delivery simulation**: simulated payment, delivery assignment/status, ETA, cancellation/refund path.
7. **Testing and polish**: unit/integration tests, Testcontainers scaffolding, API docs, Postman collection, deployment notes.

## Run Locally

```bash
docker compose up -d
mvn spring-boot:run
```

Open Swagger at `http://localhost:8080/swagger-ui.html` and the dashboard at `http://localhost:8080/`.

