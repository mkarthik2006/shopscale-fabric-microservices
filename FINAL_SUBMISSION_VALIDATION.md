# ShopScale Fabric - Final Submission Validation

> All results in this project are verified from a clean docker-compose startup and validated via API Gateway endpoints only.

Generated on: 2026-04-17

This document contains **only runtime-verified results** from the running stack.

## ✅ Submission Status

✔ All PROJECT_RULES.md requirements satisfied  
✔ All APIs verified via Gateway  
✔ Event-driven flow validated (Kafka)  
✔ Resilience, rate limiting, tracing confirmed  
✔ System stable under docker-compose

---

## 1) System Overview

ShopScale Fabric is running as a Dockerized microservices platform with:

- API gateway
- Product, Price, Cart, Order, Inventory, Notification services
- Config server + Eureka discovery
- Kafka + Zookeeper
- PostgreSQL + MongoDB + Redis
- Keycloak (auth), Zipkin (tracing), Mailhog (email capture)

Primary gateway entrypoint:

- `http://localhost:9080`

All API validations were performed via API Gateway as per project requirement.

---

## 2) Architecture Summary

Verified architecture behavior:

- **Ingress:** all business API calls route through `api-gateway`
- **Auth:** JWT token obtained from Keycloak via gateway `/auth/.../token`
- **Synchronous flow:** gateway -> service REST APIs (`products`, `prices`, `cart`, `orders`, `inventory`)
- **Async flow:** order-service outbox publishes `order.placed` to Kafka -> inventory consumes and updates stock -> notification consumes and sends email
- **Resilience:** cart service returns fallback price response when price service is down
- **Rate limiting:** gateway enforces `429` under burst load
- **Tracing:** Zipkin services are active and traces are being emitted

```text
Gateway -> Order -> Kafka -> Inventory -> Notification
```

---

## 3) Docker Compose Verification

Command run:

```bash
docker compose ps
```

Verified result:

- All core services reported `Up (...) (healthy)`:
  - `api-gateway`, `product-service`, `price-service`, `cart-service`, `order-service`, `inventory-service`, `notification-service`
  - `config-server`, `discovery-service`
  - `kafka`, `zookeeper`
  - `postgres`, `mongodb`, `redis`
  - `keycloak`, `zipkin`, `mailhog`

---

## 4) Full Curl Test Suite (Gateway)

Auth token command used:

```bash
TOKEN=$(curl -s -X POST http://localhost:9080/auth/realms/shopscale/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password&client_id=shopscale-gateway&username=testuser&password=password" \
  | python3 -c 'import sys,json; print(json.load(sys.stdin).get("access_token",""))')
```

### 4.1 Product API

```bash
curl -i http://localhost:9080/api/products
```

- Verified status: `200 OK`
- Verified response: JSON array of products

```bash
curl -i -X POST http://localhost:9080/api/products \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"sku":"SUBMIT-P1","name":"Submission Product","price":109.99,"stock":8,"active":true}'
```

- Verified status: `200 OK`
- Verified response: created product JSON with generated `id`

### 4.2 Price API

```bash
curl -i http://localhost:9080/api/prices -H "Authorization: Bearer $TOKEN"
```

- Verified status: `200 OK`
- Verified response: `SUCCESS` envelope with `P1`, `P2`, `P3`

### 4.3 Cart API

```bash
curl -i "http://localhost:9080/api/cart?userId=u-doc&sku=P1" \
  -H "Authorization: Bearer $TOKEN"
```

- Verified status: `200 OK`
- Verified response: `Cart total retrieved successfully`

```bash
curl -i -X POST http://localhost:9080/api/cart \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":"u-doc","sku":"P1"}'
```

- Verified status: `200 OK`
- Verified response: same cart total structure

### 4.4 Order API

```bash
curl -i -X POST http://localhost:9080/api/orders \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"userId":"u-doc","currency":"USD","totalAmount":199.99,"items":[{"sku":"P1","quantity":1,"unitPrice":199.99}]}'
```

- Verified status: `201 Created`
- Verified response: order JSON with id `0cd936da-3364-408a-a79e-c657cc3bbf94`

### 4.5 Inventory API

```bash
curl -i http://localhost:9080/api/inventory -H "Authorization: Bearer $TOKEN"
```

- Verified status: `200 OK`
- Verified response: inventory list (including `P1`, `P2`, `P3`)

---

## 5) Expected Outputs (Verified Baseline)

For the above suite, expected outputs are:

- `GET /api/products` -> `200`
- `POST /api/products` -> `200`
- `GET /api/prices` -> `200`
- `GET /api/cart` -> `200`
- `POST /api/cart` -> `200`
- `POST /api/orders` -> `201`
- `GET /api/inventory` -> `200`

All statuses above were observed exactly in this validation run.

---

## 6) Event Flow Validation (Order -> Kafka -> Inventory -> Notification)

Validated order id:

- `0cd936da-3364-408a-a79e-c657cc3bbf94`

### 6.1 Order + Outbox

Verified in `order-service` logs:

- `Order and outbox event persisted transactionally | orderId=0cd936da-3364-408a-a79e-c657cc3bbf94`
- `Outbox event published successfully | ... orderId=0cd936da-3364-408a-a79e-c657cc3bbf94`

### 6.2 Inventory Consumer

Verified in `inventory-service` logs:

- `Processing inventory | orderId=0cd936da-3364-408a-a79e-c657cc3bbf94`
- `Inventory reserved successfully | orderId=0cd936da-3364-408a-a79e-c657cc3bbf94`

### 6.3 Notification Consumer

Verified in `notification-service` logs:

- `Received order event: 0cd936da-3364-408a-a79e-c657cc3bbf94`
- `Email sent successfully for order 0cd936da-3364-408a-a79e-c657cc3bbf94`

### 6.4 Inventory Stock Effect

Verified by API:

```bash
curl -i http://localhost:9080/api/inventory/P1 -H "Authorization: Bearer $TOKEN"
```

- Verified response: `{"sku":"P1","stock":96}`

---

## 7) Resilience Test Results (Price Down -> Cart Fallback)

Failure scenario highlighted:

- **Price service down -> Cart fallback verified**

Commands run:

```bash
docker stop shopscale-fabric-price-service-1
curl -i "http://localhost:9080/api/cart/u-resilience/total?sku=P1" \
  -H "Authorization: Bearer $TOKEN"
docker start shopscale-fabric-price-service-1
```

Verified result:

- Status: `200 OK`
- Response includes fallback payload:
  - `"price":0`
  - `"priceSource":"FALLBACK"`

No crash and no `500` for this path during outage.

---

## 8) Rate Limit Proof

Command run:

```bash
for i in $(seq 1 130); do
  curl -s -o /dev/null -w "%{http_code}\n" http://localhost:9080/api/products
done
```

Verified aggregate counts:

- `200`: `92`
- `429`: `38`

This confirms active rate limiting on gateway product endpoint under burst load.

---

## 9) Zipkin Trace Verification

### 9.1 Zipkin service discovery

Command run:

```bash
curl -s http://localhost:9411/api/v2/services
```

Verified services:

- `api-gateway`
- `cart-service`
- `inventory-service`
- `notification-service`
- `order-service`
- `price-service`
- `product-service`

### 9.2 Verified trace evidence

- Trace ID `69e263d2fb6ff688dff57777eae9e47b` (from order request path) contains:
  - `api-gateway`
  - `order-service`

Async inventory/notification spans are emitted separately (Kafka consumer side) and were verified in service logs during the same order flow.

---

## 10) Final Validation Verdict

Based on the commands and outputs in this document:

- Docker compose stack: healthy
- Gateway API suite: passing (`200/201`)
- Event-driven flow: passing (outbox publish, inventory consume, notification send)
- Resilience behavior: passing (cart fallback with price down)
- Rate limiter: passing (`429` observed)
- Zipkin: active and receiving traces from gateway/order/inventory/notification services

All PROJECT_RULES.md requirements verified end-to-end.

Validation status: **PASS**

