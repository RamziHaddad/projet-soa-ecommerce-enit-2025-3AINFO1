from pydantic import BaseModel
from typing import Dict, Any, Optional

# Request model for recommendation endpoint
class RecommendRequest(BaseModel):
    user_id: str
    top_k: Optional[int] = 10

# Response for a single item
class ItemScore(BaseModel):
    item_id: str
    score: float

# Response model for /recommend
class RecommendResponse(BaseModel):
    user_id: str
    items: Dict[str, float]
