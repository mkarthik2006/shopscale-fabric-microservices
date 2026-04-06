# ShopScale Fabric — Event-Driven Microservices Marketplace

> Enterprise-grade E-Commerce platform built with **Java 21**, **Spring Boot 3.3**, **Apache Kafka**, **Spring Cloud**, and **Docker**.
> Designed for high scalability, fault tolerance, and cloud-native deployment.

## Architecture Overview

```
                    ┌──────────────┐
                    │   React.js   │ ← Port 3000
                    │   Frontend   │
                    └──────┬───────┘
                           │
                    ┌──────▼───────┐
                    │  API Gateway │ ← Port 9080
                    │ (JWT + Rate  │   Spring Cloud Gateway
                    │  Limiting)   │   Resilience4j Circuit Breakers
                    └──┬──┬──┬──┬──┘
                       │  │  │  │
          ┌────────────┘  │  │  └─────────────┐
          │               │  │                │
  ┌───────▼──────┐ ┌─────▼──▼─────┐  ┌───────▼──────┐
  │   Product    │ │  Order       │  │    Cart      │
  │   Service    │ │  Service     │  │   Service    │
  │  (MongoDB)   │ │ (Postgres)   │  │ (Resilience) │
  └──────────────┘ └──────┬───────┘  └──────┬───────┘
                          │                  │
                   ┌──────▼──────┐    ┌──────▼──────┐
                   │    Kafka    │    │   Price     │
                   │ order.placed│    │  Service    │
                   └──┬──────┬──┘    │  (Redis)    │
                      │      │       └─────────────┘
            ┌─────────▼┐  ┌──▼──────────┐
            │ Inventory │  │ Notification│
            │  Service  │  │   Service   │
            │(Postgres) │  │  (MailHog)  │
            └───────────┘  └─────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21 (Virtual Threads), Spring Boot 3.3.5 |
| Gateway | Spring Cloud Gateway + Resilience4j |
| Security | OAuth2/OIDC with Keycloak IdP |
| Messaging | Apache Kafka (Event-Driven / SAGA) |
| Databases | PostgreSQL 16 (Orders, Inventory), MongoDB 7 (Products) |
| Caching | Redis 7 (Rate Limiting, Price Cache) |
| Frontend | React.js 18 + Zustand |
| Tracing | Zipkin + Micrometer Brave |
| Deployment | Docker + Docker Compose |

## Quick Start

```bash
# Clone the repository
git clone https://github.com/mkarthik2006/shopscale-fabric-microservices.git
cd shopscale-fabric-microservices

# Start the entire stack
docker-compose up --build -d

# Verify all services are up
docker-compose ps
```

### Access Points

| Service | URL |
|---------|-----|
| React Frontend | http://localhost:3000 |
| API Gateway | http://localhost:9080 |
| Eureka Dashboard | http://localhost:8761 |
| Zipkin Tracing | http://localhost:9411 |
| Keycloak Admin | http://localhost:8180 (admin/admin) |
| MailHog UI | http://localhost:8025 |
| Prometheus Metrics | http://localhost:9080/actuator/prometheus |

### Default Login

| Username | Password | Role |
|----------|----------|------|
| testuser | password | ROLE_USER |
| admin | admin | ROLE_ADMIN |

## Microservices

| Service | Port | Database | Description |
|---------|------|----------|-------------|
| config-server | 8888 | — | Centralized configuration via native file repo |
| discovery-service | 8761 | — | Eureka Service Registry |
| api-gateway | 9080 | Redis | JWT validation, rate limiting, circuit breakers |
| product-service | 9081 | MongoDB | Product catalogue CRUD |
| order-service | 9082 | PostgreSQL | Order placement + Kafka producer |
| inventory-service | 9083 | PostgreSQL | Stock management + Kafka consumer |
| notification-service | 9084 | — | Email notifications via MailHog |
| price-service | 9085 | Redis | Pricing engine with @Cacheable |
| cart-service | 9086 | — | Cart aggregation with Resilience4j |

## Event-Driven Architecture (SAGA Pattern)

```
1. POST /api/orders → Order Service saves order (status=PLACED)
2. Order Service → publishes OrderPlacedEvent to Kafka topic "order.placed"
3. Inventory Service → consumes event, deducts stock
   └── If insufficient stock → publishes InventoryInsufficientEvent to "inventory.failure"
4. Notification Service → consumes event, sends email via MailHog
5. Order Service → consumes "inventory.failure", sets order status=CANCELLED
```

## Resiliency

- **Circuit Breaker**: Cart → Price Service (`@CircuitBreaker` + `@Retry`)
- **Gateway Circuit Breakers**: Product & Order routes with fallback URIs
- **Rate Limiting**: 100 req/min per user/IP via Redis
- **Idempotent Processing**: Duplicate Kafka events are skipped via ProcessedEventEntity

## Running Tests

```bash
# Run all unit tests
mvn test

# Run integration tests
mvn verify
```

## API Examples

```bash
# Create a product (public)
curl -X POST http://localhost:9080/api/products \
  -H "Content-Type: application/json" \
  -d '{"sku":"P1","name":"Widget","price":199.99,"stock":100,"active":true}'

# Get JWT token from Keycloak
TOKEN=$(curl -s -X POST http://localhost:8180/realms/shopscale/protocol/openid-connect/token \
  -d "grant_type=password&client_id=shopscale-gateway&username=testuser&password=password" \
  | jq -r '.access_token')

# Place an order (authenticated)
curl -X POST http://localhost:9080/api/orders \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer $TOKEN" \
  -d '{"userId":"testuser","items":[{"sku":"P1","quantity":2,"unitPrice":199.99}],"totalAmount":399.98,"currency":"USD"}'

# Check order status
curl http://localhost:9080/api/orders?userId=testuser -H "Authorization: Bearer $TOKEN"
```

## Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| DB_HOST | postgres | PostgreSQL hostname |
| KAFKA_BOOTSTRAP_SERVERS | kafka:9092 | Kafka broker address |
| REDIS_HOST | redis | Redis hostname |
| KEYCLOAK_HOST | keycloak | Keycloak hostname |
| EUREKA_HOST | discovery-service | Eureka hostname |

---

**Zaalima Development — Enterprise Grade or Nothing.**