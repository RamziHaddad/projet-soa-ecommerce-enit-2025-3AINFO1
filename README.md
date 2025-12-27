This repository contains `baha-hassine-recommandation`, a small Recommendation microservice
implemented with FastAPI and designed to match the architecture provided by the
team. It is intentionally self-contained and documented so you can extend it to
Getting started (commands)

1) From the repository root, create the offline DB used as fallback:

```bash
python3 scripts/create_offline_db.py
```
Notes and next steps

- Replace `scripts/mock_model_service.py` with the real model-serving endpoint
  and adjust `MODEL_SERVER_URL` in the environment.
- Integrate authentication, tracing (OpenTelemetry), and metrics (Prometheus) for production.
- Add schema migrations for the offline DB if you keep it.
**Important — Quick Reference for Evaluation (for your professor)**

- Service endpoints (local):
  - Health: `GET /health` — http://localhost:8080/health
  - Recommend (ranked items): `POST /recommend` — body: `{ "user_id": "<id>", "top_k": <n> }`
  - Recommend similar: `GET /recommend/similar?user_id=<id>&top_k=<n>`
  - Metrics (Prometheus): `GET /metrics` — http://localhost:8080/metrics
How to run the project (recommended, reproducible):

  1. Seed the offline database (creates `offline.db`):

  ```bash
  make seed-db
  # or
  python3 scripts/create_offline_db.py
  ```
Running tests (two options):

  - Inside Docker (recommended, no host Python dependency):

  ```bash
  # mounts project into container and runs pytest
  docker compose run --rm -v "$PWD":/app recommendation python -m pytest -q
  ```
# baha-hassine-recommandation

This repository contains `baha-hassine-recommandation`, a small Recommendation microservice
implemented with FastAPI and designed to match the architecture provided by the
team. It is intentionally self-contained and documented so you can extend it to
connect to your colleagues' microservices (Event Collector, Feature Store, Model Serving, etc.).

**Architecture mapping (from the diagram you provided)**
- Recommendation API: this service (exposes `/recommend`) — uses features and model service
- Feature Store Service: this service reads from Redis (online) and an offline SQLite DB
- Model Services: the service expects a separate model-serving engine (we include a mock service for local dev)

What is included
- app/: service code (FastAPI)
- scripts/: helper scripts including `mock_model_service.py` and `create_offline_db.py`
- Dockerfile and docker-compose.yml for local integration with `redis` and the mock model server
- tests/: pytest unit tests with monkeypatching to avoid external dependencies

Quick file map
- app/main.py: API endpoints and flow
- app/feature_store.py: Redis + SQLite fallback logic for retrieving features
- app/model_client.py: HTTP client to call model-serving
- scripts/mock_model_service.py: simple local model-serving mock
- scripts/create_offline_db.py: populates `offline.db` with an example user

Getting started (commands)

1) From the repository root, create the offline DB used as fallback:

```bash
python3 scripts/create_offline_db.py
```

2) Start with Docker Compose (builds the recommendation image and runs redis + mock model):

```bash
docker compose up --build
```

- The API will be available on http://localhost:8080
- Health: http://localhost:8080/health
- Recommendation example (after seeding offline DB):

```bash
curl -X POST http://localhost:8080/recommend -H 'Content-Type: application/json' -d '{"user_id":"example_user","top_k":5}'
```

Notes and next steps
- Replace `scripts/mock_model_service.py` with the real model-serving endpoint
  and adjust `MODEL_SERVER_URL` in the environment.
- Integrate authentication, tracing (OpenTelemetry), and metrics (Prometheus) for production.
- Add schema migrations for the offline DB if you keep it.

Development notes
- Run tests: `pytest -q`
- Build image locally: `docker build -t baha-hassine-recommandation .`

License: none provided (example educational project)

**Makefile**

A `Makefile` is provided to simplify common tasks:

- `make up` : build and start the stack in detached mode
- `make down` : stop the stack
- `make build` : build images
- `make test` : run unit tests
- `make logs` : show recent logs
- `make seed-db` : create `offline.db`

Examples:

```bash
make seed-db
make up
curl http://127.0.0.1:8080/health
make logs
make down
```


**Important — Quick Reference for Evaluation (for your professor)**

- Service endpoints (local):
  - Health: `GET /health` — http://localhost:8080/health
  - Recommend (ranked items): `POST /recommend` — body: `{ "user_id": "<id>", "top_k": <n> }`
  - Recommend similar: `GET /recommend/similar?user_id=<id>&top_k=<n>`
  - Metrics (Prometheus): `GET /metrics` — http://localhost:8080/metrics

- Docker Compose ports (default):
  - Recommendation API: `8080` (container) → `8080` (host)
  - Mock model server: `5001` (container) → `5001` (host)
  - Redis (online feature store): `6379` (container) → `16379` (host) to avoid local conflicts

- How to run the project (recommended, reproducible):

  1. Seed the offline database (creates `offline.db`):

  ```bash
  make seed-db
  # or
  python3 scripts/create_offline_db.py
  ```

  2. Start the stack with Docker Compose (build + run):

  ```bash
  docker compose up --build -d
  ```

  3. Verify endpoints (examples):

  ```bash
  curl http://localhost:8080/health
  curl -X POST http://localhost:8080/recommend -H 'Content-Type: application/json' -d '{"user_id":"example_user","top_k":3}'
  curl "http://localhost:8080/recommend/similar?user_id=example_user&top_k=3"
  ```

- Running tests (two options):
  - Inside Docker (recommended, no host Python dependency):

  ```bash
  # mounts project into container and runs pytest
  docker compose run --rm -v "$PWD":/app recommendation python -m pytest -q
  ```

  - Locally in a virtualenv: requires Python 3.11

  ```bash
  python3.11 -m venv .venv
  .venv/bin/python -m pip install --upgrade pip setuptools wheel
  .venv/bin/pip install -r requirements.txt
  .venv/bin/pytest -q
  ```

- Notes for grading:
  - The code exposes two recommendation methods required by the assignment:
    1. `recommend_similar_product(user_id, top_k)` — recommends items similar to the user's last-purchased product (`/recommend/similar`).
    2. `score_items_for_user(features, candidate_items, top_k)` — computes scores for candidate items (implemented in `app/recommender.py`).
  - Implementation details:
    - Feature retrieval: `app/feature_store.py` reads Redis first, then falls back to `offline.db`.
    - Model serving: `app/model_client.py` calls an external model server (a mock is provided in `scripts/mock_model_service.py`).
    - Structured logging and Prometheus metrics are included.

