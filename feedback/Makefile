# Makefile for Feedback Service
# Common commands for development and deployment

.PHONY: help install dev docker-up docker-down migrate test clean token

# Default target
help:
	@echo "Feedback Service - Available Commands"
	@echo "======================================"
	@echo ""
	@echo "Development:"
	@echo "  make install      - Install dependencies with uv"
	@echo "  make dev          - Start development server"
	@echo "  make migrate      - Run database migrations"
	@echo "  make token        - Generate test JWT tokens"
	@echo ""
	@echo "Docker:"
	@echo "  make docker-up    - Start all services with Docker"
	@echo "  make docker-down  - Stop all Docker services"
	@echo "  make docker-logs  - View application logs"
	@echo "  make docker-clean - Clean restart (removes data)"
	@echo ""
	@echo "Testing:"
	@echo "  make test         - Run tests"
	@echo "  make test-cov     - Run tests with coverage"
	@echo ""
	@echo "Cleanup:"
	@echo "  make clean        - Remove cache and build files"
	@echo ""

# Install dependencies
install:
	@echo "Installing dependencies..."
	uv venv || python -m venv venv
	. .venv/bin/activate && uv pip install -r requirements.txt || pip install -r requirements.txt
	@echo "Done! Activate venv: source .venv/bin/activate"

# Start development server
dev:
	@echo "Starting development server..."
	uvicorn app.main:app --reload --host 0.0.0.0 --port 8000

# Run database migrations
migrate:
	@echo "Running database migrations..."
	alembic upgrade head
	@echo "Migration complete!"

# Generate test tokens
token:
	@echo "Generating test JWT tokens..."
	python scripts/generate_token.py

# Docker commands
docker-up:
	@echo "Starting Docker services..."
	docker-compose up -d
	@echo "Waiting for services..."
	sleep 5
	docker-compose exec app alembic upgrade head || true
	@echo ""
	@echo "✅ Services started!"
	@echo "API: http://localhost:8000/docs"
	@echo "Generate tokens: make token"

docker-down:
	@echo "Stopping Docker services..."
	docker-compose down

docker-logs:
	docker-compose logs -f app

docker-clean:
	@echo "Clean restart (this will delete all data)..."
	docker-compose down -v
	docker-compose up -d
	sleep 5
	docker-compose exec app alembic upgrade head

docker-shell:
	docker-compose exec app /bin/bash

# Testing
test:
	@echo "Running tests..."
	pytest tests/ -v

test-cov:
	@echo "Running tests with coverage..."
	pytest tests/ --cov=app --cov-report=html --cov-report=term

# Cleanup
clean:
	@echo "Cleaning up..."
	find . -type d -name __pycache__ -exec rm -rf {} +
	find . -type f -name "*.pyc" -delete
	find . -type d -name "*.egg-info" -exec rm -rf {} +
	find . -type d -name ".pytest_cache" -exec rm -rf {} +
	rm -rf htmlcov/
	rm -rf .coverage
	@echo "Cleanup complete!"

# Database access
db-shell:
	docker-compose exec db psql -U feedback_user -d feedback_db

redis-shell:
	docker-compose exec redis redis-cli

# Code formatting
format:
	@echo "Formatting code with black..."
	black app/ tests/ --line-length 100

lint:
	@echo "Linting code with ruff..."
	ruff check app/ tests/

# Quick setup
setup: install
	@echo "Copying environment template..."
	cp .env.example .env || true
	@echo ""
	@echo "✅ Setup complete!"
	@echo ""
	@echo "Next steps:"
	@echo "  1. Edit .env with your configuration"
	@echo "  2. Run: make docker-up"
	@echo "  3. Generate tokens: make token"
	@echo "  4. Open: http://localhost:8000/docs"
