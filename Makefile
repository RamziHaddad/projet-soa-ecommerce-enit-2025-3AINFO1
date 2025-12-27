.PHONY: help up down build test logs seed-db clean

help:
	@echo "Available targets:"
	@echo "  make up       - build and start docker compose (detached)"
	@echo "  make down     - stop and remove compose stack"
	@echo "  make build    - build docker images"
	@echo "  make test     - run pytest"
	@echo "  make logs     - tail logs for services"
	@echo "  make seed-db  - create offline.db via script"
	@echo "  make clean    - remove offline.db and __pycache__"

up:
	docker compose up --build -d

down:
	docker compose down

build:
	docker compose build

test:
	pytest -q

logs:
	docker compose logs --tail 200

seed-db:
	python3 scripts/create_offline_db.py

clean:
	-rm -f offline.db
	-find . -type d -name '__pycache__' -exec rm -rf {} +
