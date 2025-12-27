# Cart Service Microservice

A Spring Boot microservice for managing shopping carts in an e-commerce system. This service handles cart operations, integrates with the order service for checkout, and implements resilience patterns and event-driven architecture using the outbox pattern.

## Overview

The Cart Service is part of a larger microservices e-commerce platform. It provides RESTful APIs for creating and managing shopping carts, adding/removing items, and checking out carts. Upon checkout, it communicates with the Order Service to create orders and uses an outbox pattern for reliable event publishing.

## Technology Stack

- **Framework**: Spring Boot 3.3.10
- **Language**: Java 17
- **Database**: PostgreSQL with Hibernate/JPA
- **Communication**: Spring Cloud OpenFeign (for Order Service)
- **Build Tool**: Maven
- **Database Types**: Hibernate Types for JSONB support

## Architecture Features

- **Outbox Pattern**: Reliable event publishing for cart events
- **Async Processing**: Background event publishing with @Async
- **RESTful API**: Standard HTTP methods and status codes
- **Synchronous Communication**: Request-response pattern for all operations

## Communication Patterns

### Synchronous Operations

The cart service operates primarily in a **synchronous** manner:

1. **REST Endpoints**: All API calls return responses immediately after processing
2. **Service Communication**: Calls to Order Service use synchronous HTTP requests via Feign client
3. **Database Operations**: All CRUD operations complete before returning responses
4. **Transaction Boundaries**: Operations wait for database commits before responding

### Request Flow Example

```
Client Request → Cart Controller → Cart Service → Database → Response
                                      ↓
                               Order Service Call (Sync)
                                      ↓
                               Order Service Response
```

### Asynchronous Capabilities

While the main operations are synchronous, the service is configured for asynchronous processing:

- `@EnableAsync` is enabled in the application configuration
- Outbox event publishing can be made asynchronous (currently synchronous)
- Framework ready for background task processing

### Current Implementation Status

**Synchronous**: ✅ Active
- REST API responses
- Order Service communication
- Database transactions
- Outbox event flushing

**Asynchronous**: ⚠️ Configured but not utilized
- `@EnableAsync` enabled
- No `@Async` methods currently implemented
- Ready for future async event publishing

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- PostgreSQL database
- Order Service running (default: http://localhost:8081)

## Configuration

### Environment Variables
- `DB_USERNAME`: PostgreSQL username
- `DB_PASSWORD`: PostgreSQL password

### Application Properties
```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/cartdb
    username: ${DB_USERNAME}
    password: ${DB_PASSWORD}

server:
  port: 8082

orderservice:
  url: http://localhost:8081
```

## API Endpoints

### Cart Management

#### 1. Create Cart
**POST** `/api/carts`

Creates a new shopping cart for a customer.

**Request Body:**
```json
{
  "customerId": "123"
}
```

**Response:**
```json
{
  "id": 1,
  "customerId": 123,
  "status": "OPEN",
  "currency": "TND",
  "totalAmount": 0.00,
  "items": [],
  "createdAt": "2025-12-26T10:00:00Z",
  "updatedAt": "2025-12-26T10:00:00Z"
}
```

**Notes:**
- `customerId` is optional in the request body
- Returns the created cart with generated ID

#### 2. Get Cart
**GET** `/api/carts/{id}`

Retrieves a cart by its ID.

**Path Parameters:**
- `id` (Long): Cart ID

**Response:**
```json
{
  "id": 1,
  "customerId": 123,
  "status": "OPEN",
  "currency": "TND",
  "totalAmount": 25.99,
  "items": [
    {
      "id": 1,
      "productId": 456,
      "name": "Sample Product",
      "unitPrice": 12.99,
      "quantity": 2,
      "lineTotal": 25.98,
      "createdAt": "2025-12-26T10:05:00Z",
      "updatedAt": "2025-12-26T10:05:00Z"
    }
  ],
  "createdAt": "2025-12-26T10:00:00Z",
  "updatedAt": "2025-12-26T10:05:00Z"
}
```

**Error Responses:**
- `404 Not Found`: Cart not found

#### 3. Add Item to Cart
**POST** `/api/carts/{id}/items`

Adds a product item to an existing cart.

**Path Parameters:**
- `id` (Long): Cart ID

**Request Body:**
```json
{
  "productId": "456",
  "name": "Sample Product",
  "unitPrice": 12.99,
  "quantity": 2
}
```

**Response:**
Returns the updated cart (same as GET cart response).

**Notes:**
- If the product already exists in the cart, the quantity is increased
- `lineTotal` is automatically calculated as `unitPrice * quantity`

#### 4. Remove Item from Cart
**DELETE** `/api/carts/{id}/items/{itemId}`

Removes a specific item from a cart.

**Path Parameters:**
- `id` (Long): Cart ID
- `itemId` (Long): Cart item ID

**Response:**
- `204 No Content`: Item successfully removed

**Error Responses:**
- `404 Not Found`: Cart or item not found

#### 5. Checkout Cart
**POST** `/api/carts/{id}/checkout`

Processes the cart checkout by creating an order in the Order Service.

**Path Parameters:**
- `id` (Long): Cart ID

**Request Body:**
None

**Response:**
```json
{
  "id": 1001,
  "orderNumber": "ORD-2025-001",
  "customerId": 123,
  "status": "PENDING",
  "totalAmount": 25.98,
  "shippingAddress": "Default Address",
  "items": [
    {
      "id": 1,
      "productId": 456,
      "quantity": 2,
      "unitPrice": 12.99,
      "subtotal": 25.98
    }
  ]
}
```

**Integration with Order Service:**
- Calls Order Service at `POST /api/orders`
- Sends `OrderRequest` with customer ID, shipping address, and items
- Receives `OrderResponse` with order details
- Updates cart status to "CHECKED_OUT"

**Error Responses:**
- `404 Not Found`: Cart not found
- `400 Bad Request`: Cart is empty
- `500 Internal Server Error`: Order Service unavailable or other server errors

### Outbox Management

#### 6. Flush Outbox Events
**POST** `/api/outbox/flush`

Manually triggers the publishing of pending outbox events.

**Request Body:**
None

**Response:**
```json
{
  "published": 5
}
```

**Notes:**
- Publishes events to external systems (message brokers, etc.)
- Returns the number of events published
- Normally called by scheduled jobs or background processes

## Data Models

### Cart
```json
{
  "id": "Long",
  "customerId": "Long",
  "status": "String (OPEN/CHECKED_OUT/ABANDONED)",
  "currency": "String (TND)",
  "totalAmount": "BigDecimal",
  "items": "List<CartItem>",
  "createdAt": "OffsetDateTime",
  "updatedAt": "OffsetDateTime"
}
```

### CartItem
```json
{
  "id": "Long",
  "productId": "Long",
  "name": "String",
  "unitPrice": "BigDecimal",
  "quantity": "Integer",
  "lineTotal": "BigDecimal",
  "createdAt": "OffsetDateTime",
  "updatedAt": "OffsetDateTime"
}
```

### OrdersOutbox (Event)
```json
{
  "id": "Long",
  "aggregateType": "String",
  "aggregateId": "Long",
  "eventType": "String",
  "payload": "String (JSON)",
  "published": "boolean",
  "publishedAt": "OffsetDateTime",
  "attempts": "int",
  "createdAt": "OffsetDateTime"
}
```

## Monitoring

### Actuator Endpoints
- `GET /actuator/health`: Service health check
- `GET /actuator/info`: Application information

## Building and Running

### Build
```bash
mvn clean compile
```

### Run
```bash
mvn spring-boot:run
```

### Run Tests
```bash
mvn test
```

## Database Schema

The service uses Hibernate auto-DDL with PostgreSQL. Key tables:
- `cart`: Cart entities
- `cart_item`: Cart item entities
- `orders_outbox`: Outbox events for reliable messaging

## Integration Points

### Order Service
- **Endpoint**: `POST /api/orders`
- **Purpose**: Create orders during checkout
- **Protocol**: HTTP via Feign client
- **Fallback**: Basic error handling (no circuit breaker)

### Future Integrations
- **Product Service**: For product validation and pricing
- **Inventory Service**: For stock checking
- **Payment Service**: For payment processing
- **Notification Service**: For order confirmations

## Development Notes

- Uses OffsetDateTime for timestamps
- Implements outbox pattern for reliable event publishing
- Async event publishing for non-blocking operations
- Basic error handling and logging
- RESTful API design following Spring Boot conventions

## Contributing

1. Follow the existing code style and patterns
2. Add tests for new functionality
3. Update this README for API changes
4. Ensure resilience patterns are applied to external calls