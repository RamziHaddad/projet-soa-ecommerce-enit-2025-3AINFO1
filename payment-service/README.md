# Payment Microservice

This is a Quarkus-based microservice for handling payments in an e-commerce application.

## Features

- REST API for processing payments
- PostgreSQL database for persistence
- Kafka for event publishing
- RabbitMQ for command handling
- Fault tolerance with Circuit Breaker and Retry
- Saga pattern for transaction coordination
- Outbox pattern for reliable event publishing
- Idempotent payment processing

## Prerequisites

- Java 17+
- Maven 3.8+
- PostgreSQL
- Kafka (optional, dev services enabled)
- RabbitMQ (optional, dev services enabled)

## Database Setup

Create a PostgreSQL database named `payment_db` with user `payment_user` and password `password`.

## Running the Application

```bash
cd payment-service
mvn quarkus:dev
```

The application will start on `http://localhost:8081`.

## Database Tables

The service creates the following tables:

### paiements
- payment_id (UUID, Primary Key)
- user_id (UUID)
- card_number (VARCHAR)
- amount (DECIMAL)
- status (VARCHAR)
- attempts (INT)
- previous_step (VARCHAR)
- next_step (VARCHAR)
- created_at (TIMESTAMP)

### outbox
- event_id (UUID, Primary Key)
- payment_id (UUID)
- event_type (VARCHAR)
- payload (TEXT)
- processed (BOOLEAN)
- created_at (TIMESTAMP)

## API Usage

### Process Payment

```bash
POST /paiement
Content-Type: application/json

{
  "paymentId": "uuid",
  "userId": "uuid",
  "cardNumber": "1234567890123456",
  "amount": 100.00
}
```

## Testing

Run tests with:

```bash
mvn test
```

## Architecture

- **Entities**: Paiement, Outbox
- **DTOs**: PaymentRequest, PaymentResponse
- **Services**: PaymentService, SagaService
- **Resource**: PaymentResource

Events are published to Kafka topic `payment-events`.
