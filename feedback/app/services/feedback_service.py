"""
Business logic layer for feedback operations
"""
from typing import Optional, List, Dict
from sqlalchemy.orm import Session
from app.repositories.feedback_repository import FeedbackRepository
from app.services.catalog_client import catalog_client
from app.schemas.feedback import FeedbackCreate, FeedbackUpdate, FeedbackResponse, FeedbackSummary
from app.models.feedback import Feedback
from app.utils.cache import cache_get, cache_set, cache_delete
from fastapi import HTTPException, status
import httpx
import logging

logger = logging.getLogger(__name__)


class FeedbackService:
    """
    Service layer for feedback business logic
    Handles validation, external service calls, and caching
    """
    
    def __init__(self, db: Session):
        self.db = db
        self.repository = FeedbackRepository(db)
    
    async def create_feedback(
        self,
        feedback_data: FeedbackCreate,
        user_id: int
    ) -> FeedbackResponse:
        """
        Create or update feedback (upsert logic)
        Validates product exists before saving
        
        Args:
            feedback_data: Feedback creation data
            user_id: ID of the user creating feedback
            
        Returns:
            FeedbackResponse: Created/updated feedback
            
        Raises:
            HTTPException: If product doesn't exist or service unavailable
        """
        # Step 1: Validate product exists in catalog service
        try:
            product_exists = await catalog_client.check_product_exists(feedback_data.product_id)
            
            if not product_exists:
                logger.warning(f"Product {feedback_data.product_id} not found in catalog")
                raise HTTPException(
                    status_code=status.HTTP_404_NOT_FOUND,
                    detail=f"Product with ID {feedback_data.product_id} not found"
                )
                
        except (httpx.RequestError, httpx.TimeoutException) as e:
            logger.error(f"Failed to verify product {feedback_data.product_id}: {e}")
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="Unable to verify product. Please try again later."
            )
        
        # Step 2: Create or update feedback (upsert)
        feedback = self.repository.create_or_update(
            product_id=feedback_data.product_id,
            user_id=user_id,
            rating=feedback_data.rating,
            comment=feedback_data.comment
        )
        
        # Step 3: Invalidate cache for this product's rating summary
        await self._invalidate_product_cache(feedback_data.product_id)
        
        logger.info(f"Feedback {feedback.id} created/updated for product {feedback_data.product_id}")
        
        return FeedbackResponse.model_validate(feedback)
    
    async def update_feedback(
        self,
        feedback_id: int,
        feedback_data: FeedbackUpdate,
        user_id: int
    ) -> FeedbackResponse:
        """
        Update existing feedback
        Users can only update their own feedback
        
        Args:
            feedback_id: ID of feedback to update
            feedback_data: Updated feedback data
            user_id: ID of the user updating feedback
            
        Returns:
            FeedbackResponse: Updated feedback
            
        Raises:
            HTTPException: If feedback not found or user unauthorized
        """
        # Get existing feedback
        feedback = self.repository.get_by_id(feedback_id)
        
        if not feedback:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Feedback with ID {feedback_id} not found"
            )
        
        # Check ownership
        if feedback.user_id != user_id:
            logger.warning(f"User {user_id} attempted to update feedback {feedback_id} owned by {feedback.user_id}")
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You can only update your own feedback"
            )
        
        # Update feedback
        updated_feedback = self.repository.update(
            feedback=feedback,
            rating=feedback_data.rating,
            comment=feedback_data.comment
        )
        
        # Invalidate cache
        await self._invalidate_product_cache(feedback.product_id)
        
        logger.info(f"Feedback {feedback_id} updated by user {user_id}")
        
        return FeedbackResponse.model_validate(updated_feedback)
    
    async def delete_feedback(self, feedback_id: int, user_id: int) -> None:
        """
        Delete feedback
        Users can only delete their own feedback
        
        Args:
            feedback_id: ID of feedback to delete
            user_id: ID of the user deleting feedback
            
        Raises:
            HTTPException: If feedback not found or user unauthorized
        """
        # Get existing feedback
        feedback = self.repository.get_by_id(feedback_id)
        
        if not feedback:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail=f"Feedback with ID {feedback_id} not found"
            )
        
        # Check ownership
        if feedback.user_id != user_id:
            logger.warning(f"User {user_id} attempted to delete feedback {feedback_id} owned by {feedback.user_id}")
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="You can only delete your own feedback"
            )
        
        product_id = feedback.product_id
        
        # Delete feedback
        self.repository.delete(feedback)
        
        # Invalidate cache
        await self._invalidate_product_cache(product_id)
        
        logger.info(f"Feedback {feedback_id} deleted by user {user_id}")
    
    async def get_product_feedback(
        self,
        product_id: int,
        page: int = 1,
        size: int = 20
    ) -> tuple[List[FeedbackResponse], int]:
        """
        Get all feedback for a product with pagination
        
        Args:
            product_id: Product ID
            page: Page number (1-indexed)
            size: Page size
            
        Returns:
            Tuple of (list of feedback responses, total count)
        """
        skip = (page - 1) * size
        feedbacks, total = self.repository.get_by_product(product_id, skip=skip, limit=size)
        
        feedback_responses = [FeedbackResponse.model_validate(f) for f in feedbacks]
        
        return feedback_responses, total
    
    async def get_user_feedback(
        self,
        user_id: int,
        page: int = 1,
        size: int = 20
    ) -> tuple[List[FeedbackResponse], int]:
        """
        Get all feedback by a user with pagination
        
        Args:
            user_id: User ID
            page: Page number (1-indexed)
            size: Page size
            
        Returns:
            Tuple of (list of feedback responses, total count)
        """
        skip = (page - 1) * size
        feedbacks, total = self.repository.get_by_user(user_id, skip=skip, limit=size)
        
        feedback_responses = [FeedbackResponse.model_validate(f) for f in feedbacks]
        
        return feedback_responses, total
    
    async def get_rating_summary(self, product_id: int) -> FeedbackSummary:
        """
        Get rating summary for a product (with caching)
        
        Args:
            product_id: Product ID
            
        Returns:
            FeedbackSummary: Rating statistics
        """
        cache_key = f"rating_summary:product:{product_id}"
        
        # Try to get from cache
        cached_data = await cache_get(cache_key)
        if cached_data:
            logger.info(f"Cache hit for product {product_id} rating summary")
            return FeedbackSummary(**cached_data)
        
        # Cache miss - calculate from database
        logger.info(f"Cache miss for product {product_id} rating summary - calculating")
        summary_data = self.repository.get_rating_summary(product_id)
        
        # Store in cache
        await cache_set(cache_key, summary_data)
        
        return FeedbackSummary(**summary_data)
    
    async def _invalidate_product_cache(self, product_id: int) -> None:
        """
        Invalidate cached rating summary for a product
        
        Args:
            product_id: Product ID
        """
        cache_key = f"rating_summary:product:{product_id}"
        await cache_delete(cache_key)
        logger.info(f"Invalidated cache for product {product_id}")
