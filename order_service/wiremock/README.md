# WireMock Mock Services for Order Service Testing

This directory contains WireMock configurations for mocking the dependent microservices needed to test the Order Service.

## Available Mock Services

- **Inventory Service**: Port 8081
- **Payment Service**: Port 8082
- **Shipping Service**: Port 8083

## Usage

### Start all services (including mocks):
```bash
docker-compose --profile full up
```

### Start only mock services:
```bash
docker-compose --profile mock up
```

### Test the setup:
- Inventory: http://localhost:8081/api/inventory/reserve
- Payment: http://localhost:8082/api/payment/process
- Shipping: http://localhost:8083/api/shipping/arrange
