"""
Feature store client helpers.
This module reads user features from an online Redis store and falls back to the
offline SQLite DB when data is missing. Redis/SQLite accesses include safe
error handling and simple retries.
"""
import os
import json
import sqlite3
from typing import Dict, Any, Optional
import redis
from tenacity import retry, stop_after_attempt, wait_exponential, retry_if_exception_type
import structlog

logger = structlog.get_logger(__name__)

REDIS_URL = os.getenv("REDIS_URL", "redis://redis:6379/0")
OFFLINE_DB = os.getenv("OFFLINE_DB", "/app/offline.db")

_redis_client: Optional[redis.Redis] = None


def get_redis_client() -> redis.Redis:
    global _redis_client
    if _redis_client is None:
        _redis_client = redis.from_url(REDIS_URL, decode_responses=True)
    return _redis_client


@retry(stop=stop_after_attempt(3), wait=wait_exponential(multiplier=1, min=1, max=5),
       retry=retry_if_exception_type(Exception))
def _redis_get(key: str) -> Optional[str]:
    r = get_redis_client()
    return r.get(key)


def get_features_for_user(user_id: str) -> Dict[str, Any]:
    """
    Try to get features for `user_id` from Redis. If not found or on error,
    fall back to the offline SQLite DB.
    """
    key = f"user:features:{user_id}"
    try:
        raw = _redis_get(key)
        if raw:
            try:
                return json.loads(raw)
            except Exception:
                logger.warning("redis_payload_invalid", user_id=user_id)
                return {}
    except Exception as e:
        logger.warning("redis_unavailable", error=str(e))

    # Fallback to offline DB
    if os.path.exists(OFFLINE_DB):
        try:
            conn = sqlite3.connect(OFFLINE_DB)
            cur = conn.cursor()
            cur.execute("SELECT features_json FROM user_features WHERE user_id = ?", (user_id,))
            row = cur.fetchone()
            if row and row[0]:
                try:
                    return json.loads(row[0])
                except Exception:
                    logger.warning("offline_payload_invalid", user_id=user_id)
                    return {}
        except Exception as e:
            logger.error("offline_db_error", error=str(e))
        finally:
            try:
                conn.close()
            except Exception:
                pass

    return {}
