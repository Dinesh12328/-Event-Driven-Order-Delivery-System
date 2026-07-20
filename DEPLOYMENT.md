# Online Deployment Guide

This app deploys cleanly as a Dockerized Spring Boot service plus managed infrastructure.

## Required Services

- PostgreSQL database
- Redis instance
- Kafka cluster
- Docker-capable app host

Managed options that work well:

- App: Render, Railway, Fly.io, AWS ECS, Google Cloud Run, or Azure Container Apps
- PostgreSQL: platform-managed PostgreSQL, Supabase, Neon, AWS RDS, or Google Cloud SQL
- Redis: Upstash, Redis Cloud, AWS ElastiCache, or platform-managed Redis
- Kafka: Confluent Cloud, Aiven, Upstash Kafka, Redpanda Cloud, or AWS MSK

## Environment Variables

Set these in the host dashboard:

```bash
DB_URL=jdbc:postgresql://<host>:<port>/<database>
DB_USERNAME=<user>
DB_PASSWORD=<password>
REDIS_HOST=<redis-host>
REDIS_PORT=6379
REDIS_PASSWORD=<redis-password-if-required>
REDIS_SSL_ENABLED=false
KAFKA_BOOTSTRAP_SERVERS=<broker-list>
KAFKA_ENABLED=true
JWT_SECRET=<long-random-secret-at-least-32-characters>
JWT_EXPIRATION_MINUTES=120
```

For a free portfolio deployment, set `KAFKA_ENABLED=false` first and use the admin event log as the event audit trail. Add managed Kafka later when you want the full async flow online.

For Kafka providers that require SASL/SSL, add the provider-specific Spring Kafka properties in `application.yml` or set them as environment variables.

## Docker Deployment

1. Provision PostgreSQL, Redis, and Kafka.
2. Push this repository to GitHub.
3. Create a new Docker web service on your host and point it at this repo.
4. Set the environment variables above.
5. Deploy the container.
6. Open `/api/health` and `/swagger-ui.html`.

## Local Production-Like Run

```bash
docker compose up -d
mvn spring-boot:run
```

## Useful URLs

- Dashboard UI: `/`
- Health check: `/api/health`
- Swagger/OpenAPI: `/swagger-ui.html`
- Admin dashboard API: `/api/admin/dashboard`
- Event audit API: `/api/admin/events`
