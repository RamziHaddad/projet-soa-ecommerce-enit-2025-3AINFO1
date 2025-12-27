# Wael ETTEYEB - Khalifa ABDALLAH
# Feedback Microservice 

A production-ready microservice for managing user feedback (ratings and comments) in an e-commerce platform. Built with Python, FastAPI, PostgreSQL, and Redis.

## 📋 Table of Contents

- [Overview](#overview)
- [Features](#features)
- [Architecture](#architecture)
- [Technology Stack](#technology-stack)
- [Prerequisites](#prerequisites)
- [Quick Start](#quick-start)
- [API Documentation](#api-documentation)
- [Testing](#testing)
- [Project Structure](#project-structure)
- [Configuration](#configuration)

## 🎯 Overview

This microservice provides a complete feedback management system where users can submit ratings (1-5 stars) and/or comments for products. It features smart upsert logic (one feedback per user per product), Redis caching for performance, JWT authentication for security, and integration with an external Catalog Service for product validation.

### Key Capabilities

- **Flexible Feedback**: Submit rating only, comment only, or both
- **Smart Upsert**: Automatically updates existing feedback instead of creating duplicates
- **Rating Aggregation**: Real-time calculation of average ratings and distribution
- **Performance**: Redis caching with automatic invalidation
- **Security**: JWT-based authentication with user authorization
- **Resilience**: Retry logic with exponential backoff for external service calls
- **Scalability**: Stateless design with connection pooling

## ✨ Features

### Core Functionality

1. **Create/Update Feedback** - Users can submit or update their feedback for products
2. **Rating System** - 1-5 star ratings with validation
3. **Comment System** - Text comments (1-2000 characters)
4. **Rating Summaries** - Aggregated statistics with distribution
5. **User Authorization** - Users can only modify their own feedback
6. **Product Validation** - Verifies product existence via Catalog Service

### Technical Features

- **Upsert Logic**: Unique constraint on (user_id, product_id) prevents duplicates
- **Redis Caching**: Rating summaries cached for 1 hour
- **Cache Invalidation**: Automatic cache clearing on feedback updates
- **Retry Mechanism**: 3 attempts with exponential backoff (1s, 2s, 4s)
- **Pagination**: Efficient listing with page/size parameters
- **Health Checks**: Readiness and liveness endpoints
- **Database Migrations**: Alembic for version control

## 🏗 Architecture

### System Architecture

```
┌─────────────┐         ┌──────────────────┐         ┌─────────────┐
│   Client    │◄───────►│  Feedback API    │◄───────►│ PostgreSQL  │
│ (Frontend)  │  HTTP   │    (FastAPI)     │  SQL    │  Database   │
└─────────────┘         └──────────────────┘         └─────────────┘
                               │      ▲
                               │      │
                     ┌─────────┘      └──────────┐
                     │                            │
                     ▼                            ▼
              ┌─────────────┐           ┌─────────────┐
              │    Redis    │           │  Catalog    │
              │   (Cache)   │           │  Service    │
              └─────────────┘           └─────────────┘
```

### Database Schema

```
feedback
├── id (SERIAL PRIMARY KEY)
├── product_id (INTEGER NOT NULL)
├── user_id (INTEGER NOT NULL)
├── rating (INTEGER, CHECK: 1-5)
├── comment (TEXT, CHECK: 1-2000 chars)
├── created_at (TIMESTAMP)
├── updated_at (TIMESTAMP)
│
├── UNIQUE(product_id, user_id)
├── CHECK(rating IS NOT NULL OR comment IS NOT NULL)
└── CHECK(rating >= 1 AND rating <= 5)

Indexes:
- idx_feedback_product_id (product_id)
- idx_feedback_user_id (user_id)
- idx_feedback_created_at (created_at DESC)
```

### Application Layers

```
┌──────────────────────────────────────────┐
│           API Layer (Routers)            │  ← HTTP endpoints
├──────────────────────────────────────────┤
│       Business Logic (Services)          │  ← Caching, validation
├──────────────────────────────────────────┤
│       Data Access (Repositories)         │  ← Database operations
├──────────────────────────────────────────┤
│            Models & Schemas              │  ← Data structures
└──────────────────────────────────────────┘
```

## 🛠 Technology Stack

| Component | Technology | Version |
|-----------|-----------|---------|
| Language | Python | 3.11+ |
| Framework | FastAPI | Latest |
| Database | PostgreSQL | 15+ |
| Cache | Redis | 7+ |
| ORM | SQLAlchemy | 2.0+ |
| Migrations | Alembic | Latest |
| Validation | Pydantic | v2 |
| Authentication | python-jose | Latest |
| HTTP Client | httpx | Latest |
| Retry Logic | tenacity | Latest |
| Containerization | Docker | Latest |

## 📦 Prerequisites

Before running this microservice, ensure you have:

- **Docker** and **Docker Compose** installed
- **Git** for cloning the repository
- Ports available: `8000` (API), `5432` (PostgreSQL), `6379` (Redis)

## 🚀 Quick Start
cd feedback

# Copy environment file
cp .env.example .env

# Review and update .env if needed (defaults work for development)
```

### 2. Configure Environment

The `.env` file contains all configuration. Key settings:

```bash
# Database
DATABASE_URL=postgresql://feedback_user:feedback_pass@localhost:5432/feedback_db

# Redis
REDIS_URL=redis://localhost:6379/0

# JWT Secret (CHANGE THIS IN PRODUCTION!)
JWT_SECRET_KEY=dev-secret-key-change-in-production-use-openssl-rand-hex-32

# External Catalog Service (update with your actual service URL)
CATALOG_SERVICE_URL=http://catalog-service:8000/api
```

### 3. Start the Services

```bash
# Build and start all services
docker-compose up -d

# Wait for services to be healthy (about 30 seconds)
docker-compose ps

# Check logs if needed
docker-compose logs -f app
```

### 4. Run Database Migrations

```bash
# Apply database schema
docker-compose exec app alembic upgrade head
```

### 5. Verify Installation

```bash
# Check health endpoint
curl http://localhost:8000/health

# Expected response:
# {"status":"healthy","service":"Feedback Service","version":"1.0.0"}
```

## 📚 API Documentation

### Interactive Documentation

Once the service is running, access the interactive API documentation:

- **Swagger UI**: http://localhost:8000/docs
- **ReDoc**: http://localhost:8000/redoc

### API Endpoints

#### Public Endpoints (No Authentication)

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/health` | Health check |
| GET | `/` | Service information |
| GET | `/api/v1/feedback/products/{product_id}` | List feedback for a product |
| GET | `/api/v1/feedback/products/{product_id}/summary` | Get rating summary (cached) |

#### Protected Endpoints (Requires JWT)

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/v1/feedback` | Create or update feedback |
| PUT | `/api/v1/feedback/{id}` | Update own feedback |
| DELETE | `/api/v1/feedback/{id}` | Delete own feedback |
| GET | `/api/v1/feedback/users/me` | Get current user's feedback |

### Request/Response Examples

#### Create Feedback

```bash
POST /api/v1/feedback
Authorization: Bearer <jwt_token>
Content-Type: application/json

{
  "product_id": 123,
  "rating": 5,
  "comment": "Excellent product!"
}

# Response (201 Created)
{
  "id": 1,
  "product_id": 123,
  "user_id": 1,
  "rating": 5,
  "comment": "Excellent product!",
  "created_at": "2025-12-27T10:00:00Z",
  "updated_at": "2025-12-27T10:00:00Z"
}
```

#### Get Rating Summary

```bash
GET /api/v1/feedback/products/123/summary

# Response (200 OK)
{
  "product_id": 123,
  "average_rating": 4.5,
  "total_ratings": 2,
  "rating_distribution": {
    "1": 0,
    "2": 0,
    "3": 0,
    "4": 1,
    "5": 1
  }
}
```

#### List Product Feedback

```bash
GET /api/v1/feedback/products/123?page=1&page_size=10

# Response (200 OK)
{
  "items": [
    {
      "id": 2,
      "product_id": 123,
      "user_id": 2,
      "rating": 4,
      "comment": "Good quality",
      "created_at": "2025-12-27T10:05:00Z",
      "updated_at": "2025-12-27T10:05:00Z"
    },
    {
      "id": 1,
      "product_id": 123,
      "user_id": 1,
      "rating": 5,
      "comment": "Excellent product!",
      "created_at": "2025-12-27T10:00:00Z",
      "updated_at": "2025-12-27T10:00:00Z"
    }
  ],
  "total": 2,
  "page": 1,
  "size": 10,
  "pages": 1
}
```

## 🧪 Testing

### Generate Test JWT Tokens

```bash
# Generate tokens for test users
docker-compose exec app python scripts/generate_token.py

# This creates tokens for:
# - alice (user_id: 1)
# - bob (user_id: 2)
# - admin (user_id: 999)
```

### Manual Testing with cURL

```bash
# Set token variable
TOKEN="<jwt_token_from_generate_token_script>"

# Create feedback
curl -X POST http://localhost:8000/api/v1/feedback \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"product_id": 123, "rating": 5, "comment": "Great!"}'

# Get rating summary
curl http://localhost:8000/api/v1/feedback/products/123/summary

# Get user's feedback
curl http://localhost:8000/api/v1/feedback/users/me \
  -H "Authorization: Bearer $TOKEN"
```

### Postman Collection

Import the provided `postman_collection.json` for a complete set of API tests.

### Test Scenarios

1. **Upsert Behavior**: Submit feedback twice for same user+product
2. **Authorization**: Try to modify another user's feedback (should fail)
3. **Validation**: Try invalid rating (0, 6) or too long comment
4. **Flexible Submission**: Submit rating-only or comment-only
5. **Cache Behavior**: Check summary response time (cached vs fresh)
6. **Pagination**: Create multiple feedbacks and test pagination

## 📁 Project Structure

```
feedback/
├── app/
│   ├── __init__.py
│   ├── main.py                    # FastAPI application entry point
│   ├── config.py                  # Configuration management
│   ├── database.py                # Database connection & session
│   │
│   ├── models/
│   │   └── feedback.py            # SQLAlchemy models
│   │
│   ├── schemas/
│   │   └── feedback.py            # Pydantic schemas (validation)
│   │
│   ├── repositories/
│   │   └── feedback_repository.py # Data access layer
│   │
│   ├── services/
│   │   ├── feedback_service.py    # Business logic
│   │   └── catalog_client.py      # External service client
│   │
│   ├── routers/
│   │   └── feedback_router.py     # API endpoints
│   │
│   ├── middleware/
│   │   └── auth_middleware.py     # JWT authentication
│   │
│   └── utils/
│       ├── cache.py               # Redis utilities
│       └── security.py            # JWT utilities
│
├── alembic/
│   ├── versions/                  # Database migrations
│   └── env.py                     # Alembic configuration
│
├── scripts/
│   └── generate_token.py          # JWT token generator
│
├── tests/
│   └── test_feedback.py           # Unit/integration tests
│
├── docker-compose.yml             # Multi-service orchestration
├── Dockerfile                     # Application container
├── requirements.txt               # Python dependencies
├── pyproject.toml                 # Project metadata
├── alembic.ini                    # Alembic configuration
├── Makefile                       # Development commands
├── .env                           # Environment variables (dev)
├── .env.example                   # Environment template
├── .gitignore                     # Git ignore rules
├── postman_collection.json        # API test collection
└── README.md                      # This file
```

## ⚙️ Configuration

### Environment Variables

| Variable | Description | Default |
|----------|-------------|---------|
| `DATABASE_URL` | PostgreSQL connection string | `postgresql://feedback_user:...` |
| `REDIS_URL` | Redis connection string | `redis://localhost:6379/0` |
| `JWT_SECRET_KEY` | Secret key for JWT signing | `dev-secret-key-...` |
| `JWT_ALGORITHM` | JWT signing algorithm | `HS256` |
| `JWT_ACCESS_TOKEN_EXPIRE_MINUTES` | Token expiration time | `30` |
| `CATALOG_SERVICE_URL` | External catalog service URL | `http://catalog-service:8000/api` |
| `APP_NAME` | Application name | `Feedback Service` |
| `APP_VERSION` | Application version | `1.0.0` |
| `DEBUG` | Enable debug mode | `False` |
| `CACHE_TTL_SECONDS` | Cache TTL for summaries | `3600` |
| `MAX_RETRY_ATTEMPTS` | Catalog service retry attempts | `3` |
| `RETRY_WAIT_MIN` | Min retry wait (seconds) | `1` |
| `RETRY_WAIT_MAX` | Max retry wait (seconds) | `4` |
| `REQUEST_TIMEOUT` | HTTP request timeout | `2.0` |

### Docker Services

The `docker-compose.yml` defines these services:

- **app**: FastAPI application (port 8000)
- **db**: PostgreSQL database (port 5432)
- **redis**: Redis cache (port 6379)
- **migration**: One-time migration runner

### Accessing Database

```bash
docker-compose exec db psql -U feedback_user -d feedback_db
```

### Accessing Redis

```bash
docker-compose exec redis redis-cli
```

### Rebuilding After Code Changes

```bash
docker-compose up -d --build app
```

### Resetting Database

```bash
docker-compose down -v  # Remove volumes
docker-compose up -d
docker-compose exec app alembic upgrade head
```
