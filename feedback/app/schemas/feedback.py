"""
Pydantic schemas for request/response validation
"""
from pydantic import BaseModel, Field, validator
from typing import Optional
from datetime import datetime


class FeedbackCreate(BaseModel):
    """
    Schema for creating new feedback
    Supports flexible submission: rating only, comment only, or both
    """
    product_id: int = Field(..., gt=0, description="Product ID from catalog service")
    rating: Optional[int] = Field(None, ge=1, le=5, description="Rating from 1 to 5 stars")
    comment: Optional[str] = Field(None, min_length=1, max_length=2000, description="Review comment")
    
    @validator('comment')
    def validate_comment(cls, v):
        """Strip whitespace from comment"""
        if v is not None:
            v = v.strip()
            if len(v) == 0:
                return None
        return v
    
    @validator('rating', 'comment')
    def at_least_one_field(cls, v, values):
        """Ensure at least one field (rating or comment) is provided"""
        if 'rating' in values and values['rating'] is None and v is None:
            raise ValueError('At least one of rating or comment must be provided')
        return v


class FeedbackUpdate(BaseModel):
    """
    Schema for updating existing feedback
    All fields are optional, but at least one must be provided
    """
    rating: Optional[int] = Field(None, ge=1, le=5, description="Updated rating from 1 to 5 stars")
    comment: Optional[str] = Field(None, min_length=1, max_length=2000, description="Updated review comment")
    
    @validator('comment')
    def validate_comment(cls, v):
        """Strip whitespace from comment"""
        if v is not None:
            v = v.strip()
            if len(v) == 0:
                return None
        return v


class FeedbackResponse(BaseModel):
    """
    Schema for feedback response
    """
    id: int
    product_id: int
    user_id: int
    rating: Optional[int]
    comment: Optional[str]
    created_at: datetime
    updated_at: datetime
    
    class Config:
        from_attributes = True  # Pydantic v2 - allows ORM model conversion


class FeedbackSummary(BaseModel):
    """
    Schema for product rating summary (cached)
    """
    product_id: int
    average_rating: float = Field(..., description="Average rating rounded to 2 decimals")
    total_ratings: int = Field(..., description="Total number of ratings")
    rating_distribution: dict[int, int] = Field(
        ..., 
        description="Count of ratings for each star level (1-5)"
    )


class FeedbackListResponse(BaseModel):
    """
    Schema for paginated feedback list
    """
    items: list[FeedbackResponse]
    total: int
    page: int
    size: int
    pages: int


class ErrorResponse(BaseModel):
    """
    Standard error response schema
    """
    detail: str
    error_code: str
    timestamp: datetime = Field(default_factory=datetime.utcnow)
