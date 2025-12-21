# Inventory Service - Saga Orchestrated E-Commerce

## Overview

This is the Inventory Service for a SOA-based e-commerce application using **Saga Orchestration** pattern.

## Architecture

- **Pattern**: Saga Orchestration (NOT Choreography)
- **Role**: Command-driven service controlled by Saga Orchestrator
- **Database**: PostgreSQL with two-counter inventory model
- **Messaging**: Kafka for event publishing

## Key Features

- ✅ Reserve inventory (idempotent)
- ✅ Cancel reservation (compensation)
- ✅ Confirm reservation (final step)
- ✅ Two-counter pattern: `available_quantity` + `reserved_quantity`
- ✅ Pessimistic locking for concurrency control
- ✅ Dockerized deployment
 - ✅ Kafka saga orchestration handlers (commands + events)
 - ✅ Notification events to communication service
 - ✅ Optional fetch of order items from Orders Service

## API Endpoints

### Reserve Inventory

```bash
POST /inventory/reservations
Content-Type: application/json

{
  "orderId": "ORD-123",
  "items": [
    { "productId": "PROD-001", "quantity": 2 }
  ]
}
```

### Cancel Reservation

```bash
POST /inventory/reservations/{orderId}/cancel
```

### Confirm Reservation

```bash
POST /inventory/reservations/{orderId}/confirm
```

## Running Locally

### With Docker Compose

```bash
docker-compose up -d
```

### Access

- Service: http://localhost:8082
- Health: http://localhost:8082/actuator/health

### Quick Test (REST)

```bash
# List inventory
curl -s http://localhost:8082/inventory | jq .[0]

# Reserve 3 units of PROD-001 for order ORD-1001
curl -s -H 'Content-Type: application/json' \
  -d '{"orderId":"ORD-1001","items":[{"productId":"PROD-001","quantity":3}]}' \
  http://localhost:8082/inventory/reservations | jq

# Confirm reservation
curl -s -X POST http://localhost:8082/inventory/reservations/ORD-1001/confirm -i

# Cancel (idempotent if already confirmed)
curl -s -X POST http://localhost:8082/inventory/reservations/ORD-1001/cancel -i
```

### Kafka Topics (Saga)

- Commands in: `inventory.reserve.command`, `inventory.cancel.command`, `inventory.confirm.command`
- Events out: `inventory.events`
- Notifications out: `comm.notifications`

You can override topic names with env vars: `SAGA_TOPIC_*`.

## Database Schema

- **inventory**: Two-counter model with optimistic locking
- **reservations**: Tracks reservation state (RESERVED/CONFIRMED/CANCELLED)

## Next Steps

- [ ] Add integration tests
- [ ] Expand error handling and retries
- [ ] End-to-end saga testing with orchestrator
