"""
Recommendation API: exposes `/recommend` and `/metrics`.
"""
from fastapi import FastAPI, HTTPException
from fastapi.responses import JSONResponse, PlainTextResponse
from fastapi.middleware.cors import CORSMiddleware
from typing import Dict

from .models import RecommendRequest, RecommendResponse
from . import feature_store
from . import model_client

import structlog
from prometheus_client import Counter, generate_latest, CONTENT_TYPE_LATEST

structlog.configure(processors=[structlog.processors.JSONRenderer()])
logger = structlog.get_logger()

app = FastAPI(title="baha-hassine-recommandation", version="0.1.0")

# Prometheus metrics
REQUESTS = Counter('recommend_requests_total', 'Total recommendation requests', ['status'])

# Enable simple CORS for local development/testing
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)


@app.post("/recommend", response_model=RecommendResponse)
async def recommend(req: RecommendRequest):
    user_id = req.user_id
    top_k = req.top_k or 10

    logger.info("recommend_request", user_id=user_id, top_k=top_k)

    # 1) Read features
    features = feature_store.get_features_for_user(user_id)

    if not features:
        REQUESTS.labels(status='no_features').inc()
        raise HTTPException(status_code=404, detail=f"No features found for user {user_id}")

    # 3) Ask model-serving engine for a ranking
    try:
        prediction = await model_client.get_prediction(features, top_k=top_k)
    except Exception as e:
        REQUESTS.labels(status='model_error').inc()
        raise HTTPException(status_code=502, detail=f"Model-serving error: {str(e)}")

    if not isinstance(prediction, dict):
        REQUESTS.labels(status='invalid_model_resp').inc()
        raise HTTPException(status_code=502, detail="Invalid response from model-serving engine")

    REQUESTS.labels(status='success').inc()
    return RecommendResponse(user_id=user_id, items=prediction)


@app.get("/health")
async def health():
    return JSONResponse({"status": "ok"})


@app.get('/metrics')
async def metrics():
    data = generate_latest()
    return PlainTextResponse(data.decode('utf-8'), media_type=CONTENT_TYPE_LATEST)

from . import recommender

@app.get("/recommend/similar")
async def recommend_similar(user_id: str, top_k: int = 5):
    """Recommend items similar to the user's last purchased product."""
    res = await recommender.recommend_similar_product(user_id, top_k=top_k)
    if not res:
        raise HTTPException(status_code=404, detail=f"No similar recommendations for user {user_id}")
    return RecommendResponse(user_id=user_id, items=res)
