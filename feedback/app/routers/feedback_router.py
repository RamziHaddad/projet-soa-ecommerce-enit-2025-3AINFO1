"""
API Router for Feedback endpoints
"""
from fastapi import APIRouter, Depends, HTTPException, status, Query
from sqlalchemy.orm import Session
from typing import Dict
from math import ceil

from app.database import get_db
from app.services.feedback_service import FeedbackService
from app.schemas.feedback import (
    FeedbackCreate,
    FeedbackUpdate,
    FeedbackResponse,
    FeedbackSummary,
    FeedbackListResponse
)
from app.middleware.auth_middleware import get_current_user
import logging

logger = logging.getLogger(__name__)

router = APIRouter()


@router.post(
    "/feedback",
    response_model=FeedbackResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Submit feedback (rating and/or comment)",
    description="Create or update feedback for a product. Supports rating only, comment only, or both."
)
async def create_feedback(
    feedback_data: FeedbackCreate,
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Submit feedback for a product
    
    **Authentication required**
    
    - **product_id**: Product ID (must exist in catalog)
    - **rating**: Optional rating from 1-5 stars
    - **comment**: Optional review comment (1-2000 characters)
    - At least one of rating or comment must be provided
    
    **Upsert Logic**: If user already has feedback for this product, it will be updated
    """
    user_id = current_user["user_id"]
    service = FeedbackService(db)
    
    try:
        feedback = await service.create_feedback(feedback_data, user_id)
        return feedback
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error creating feedback: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )


@router.put(
    "/feedback/{feedback_id}",
    response_model=FeedbackResponse,
    summary="Update own feedback",
    description="Update your own feedback. You can only update feedback you created."
)
async def update_feedback(
    feedback_id: int,
    feedback_data: FeedbackUpdate,
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Update existing feedback
    
    **Authentication required**
    **Authorization**: Can only update your own feedback
    
    - **feedback_id**: ID of feedback to update
    - **rating**: Optional updated rating (1-5 stars)
    - **comment**: Optional updated comment (1-2000 characters)
    """
    user_id = current_user["user_id"]
    service = FeedbackService(db)
    
    try:
        feedback = await service.update_feedback(feedback_id, feedback_data, user_id)
        return feedback
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error updating feedback: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )


@router.delete(
    "/feedback/{feedback_id}",
    status_code=status.HTTP_204_NO_CONTENT,
    summary="Delete own feedback",
    description="Delete your own feedback. You can only delete feedback you created."
)
async def delete_feedback(
    feedback_id: int,
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Delete existing feedback
    
    **Authentication required**
    **Authorization**: Can only delete your own feedback
    
    - **feedback_id**: ID of feedback to delete
    """
    user_id = current_user["user_id"]
    service = FeedbackService(db)
    
    try:
        await service.delete_feedback(feedback_id, user_id)
        return None
    except HTTPException:
        raise
    except Exception as e:
        logger.error(f"Error deleting feedback: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )


@router.get(
    "/feedback/products/{product_id}",
    response_model=FeedbackListResponse,
    summary="Get all feedback for a product",
    description="Public endpoint - retrieve paginated feedback for a specific product"
)
async def get_product_feedback(
    product_id: int,
    page: int = Query(1, ge=1, description="Page number (1-indexed)"),
    size: int = Query(20, ge=1, le=100, description="Page size (max 100)"),
    db: Session = Depends(get_db)
):
    """
    Get all feedback for a product with pagination
    
    **Public endpoint** - No authentication required
    
    - **product_id**: Product ID
    - **page**: Page number (default: 1)
    - **size**: Items per page (default: 20, max: 100)
    
    Returns feedback sorted by newest first
    """
    service = FeedbackService(db)
    
    try:
        feedbacks, total = await service.get_product_feedback(product_id, page, size)
        
        return FeedbackListResponse(
            items=feedbacks,
            total=total,
            page=page,
            size=size,
            pages=ceil(total / size) if total > 0 else 0
        )
    except Exception as e:
        logger.error(f"Error getting product feedback: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )


@router.get(
    "/feedback/products/{product_id}/summary",
    response_model=FeedbackSummary,
    summary="Get rating summary for a product",
    description="Public endpoint - retrieve cached rating statistics for a product"
)
async def get_rating_summary(
    product_id: int,
    db: Session = Depends(get_db)
):
    """
    Get rating summary for a product (cached)
    
    **Public endpoint** - No authentication required
    
    - **product_id**: Product ID
    
    Returns:
    - Average rating (rounded to 2 decimals)
    - Total number of ratings
    - Rating distribution (count per star level)
    
    **Cache**: Results are cached in Redis for 1 hour
    """
    service = FeedbackService(db)
    
    try:
        summary = await service.get_rating_summary(product_id)
        return summary
    except Exception as e:
        logger.error(f"Error getting rating summary: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )


@router.get(
    "/feedback/users/me",
    response_model=FeedbackListResponse,
    summary="Get current user's feedback",
    description="Get all feedback submitted by the authenticated user"
)
async def get_my_feedback(
    page: int = Query(1, ge=1, description="Page number (1-indexed)"),
    size: int = Query(20, ge=1, le=100, description="Page size (max 100)"),
    current_user: Dict = Depends(get_current_user),
    db: Session = Depends(get_db)
):
    """
    Get all feedback submitted by current user
    
    **Authentication required**
    
    - **page**: Page number (default: 1)
    - **size**: Items per page (default: 20, max: 100)
    
    Returns feedback sorted by newest first
    """
    user_id = current_user["user_id"]
    service = FeedbackService(db)
    
    try:
        feedbacks, total = await service.get_user_feedback(user_id, page, size)
        
        return FeedbackListResponse(
            items=feedbacks,
            total=total,
            page=page,
            size=size,
            pages=ceil(total / size) if total > 0 else 0
        )
    except Exception as e:
        logger.error(f"Error getting user feedback: {e}")
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail="Internal server error"
        )
