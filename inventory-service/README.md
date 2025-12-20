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

## Database Schema

- **inventory**: Two-counter model with optimistic locking
- **reservations**: Tracks reservation state (RESERVED/CONFIRMED/CANCELLED)

## Next Steps

- [ ] Add Kafka event publishing
- [ ] Implement query endpoints
- [ ] Add comprehensive error handling
- [ ] Integration tests
- [ ] Communication with Order Service
