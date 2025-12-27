"""
HTTP client to call model-serving engine with retries and timeouts.
Uses `httpx` async client and `tenacity` for retry/backoff.
"""
import os
from typing import Dict, Any
import httpx
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import structlog

logger = structlog.get_logger(__name__)

MODEL_SERVER_URL = os.getenv("MODEL_SERVER_URL", "http://mock-model:5001/predict")

@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=1, max=10),
       retry=retry_if_exception_type((httpx.RequestError, httpx.HTTPStatusError)))
async def get_prediction(features: Dict[str, Any], top_k: int = 10) -> Dict[str, float]:
    """
    Sends features to the model-serving endpoint and returns a mapping
    item_id -> score. Retries transient errors.
    """
    timeout = httpx.Timeout(5.0, connect=2.0)
    async with httpx.AsyncClient(timeout=timeout) as client:
        logger.info("calling_model_server", url=MODEL_SERVER_URL, top_k=top_k)
        resp = await client.post(MODEL_SERVER_URL, json={"features": features, "top_k": top_k})
        resp.raise_for_status()
        data = resp.json()
        logger.info("model_response", status_code=resp.status_code, items_count=len(data) if isinstance(data, dict) else 0)
        return data
