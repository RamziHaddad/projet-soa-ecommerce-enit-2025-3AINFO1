"""
Recommender utilities:
- `recommend_similar_product(user_id, top_k)` : recommend items similar to a product the user bought
- `score_items_for_user(features, candidate_items=None, top_k=10)` : calculate scores for items (uses model client with fallback heuristic)
"""
from typing import Dict, Any, List
import structlog

from . import feature_store
from . import model_client

logger = structlog.get_logger(__name__)

async def score_items_for_user(features: Dict[str, Any], candidate_items: List[str] = None, top_k: int = 10) -> Dict[str, float]:
    """
    Calculate scores for candidate items for a given user's features.
    Primary path: call external model-serving via `model_client.get_prediction`.
    Fallback: simple heuristic scoring when model is unavailable.
    """
    try:
        # If model supports passing candidate_items, include them
        payload = dict(features)
        if candidate_items is not None:
            payload['candidate_items'] = candidate_items
        return await model_client.get_prediction(payload, top_k=top_k)
    except Exception as e:
        logger.warning("model_unavailable_fallback", error=str(e))
        # heuristic fallback: if user has history, score items by recency and id hash
        scores: Dict[str, float] = {}
        history = features.get('history', []) or []
        if candidate_items is None:
            # build synthetic candidates from history
            candidate_items = [f"item_sim_{i}" for i in range(1, top_k+1)]
        for i, item in enumerate(candidate_items):
            # base score on whether item appears in history and position
            score = 0.0
            if item in history:
                score += 1.0
                score += (len(history) - history.index(item)) * 0.1
            # small deterministic component
            score += 1.0 / (1 + hash(item) % 100)
            scores[item] = score
        # return top_k sorted mapping
        top_items = dict(sorted(scores.items(), key=lambda kv: kv[1], reverse=True)[:top_k])
        return top_items

async def recommend_similar_product(user_id: str, top_k: int = 5) -> Dict[str, float]:
    """
    Recommend items similar to the user's most recently purchased product.
    Strategy:
    - read features
    - pick last item from `features['history']`
    - request model for 'similar' recommendations, passing `base_item`
    - fallback: generate synthetic similar ids
    """
    features = feature_store.get_features_for_user(user_id)
    if not features:
        return {}

    history = features.get('history', []) or []
    if not history:
        return {}

    base_item = history[-1]
    # augment features to indicate similarity request
    payload = dict(features)
    payload['base_item'] = base_item
    payload['mode'] = 'similar'
    try:
        return await model_client.get_prediction(payload, top_k=top_k)
    except Exception as e:
        logger.warning("similar_fallback", error=str(e), base_item=base_item)
        # fallback: fabricate 'similar' items deterministically
        items = {f"{base_item}_sim_{i}": 1.0/(i+1) for i in range(top_k)}
        return items
