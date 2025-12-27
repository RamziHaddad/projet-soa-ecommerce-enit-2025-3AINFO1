"""
Repository layer for database operations
Handles all direct database interactions
"""
from sqlalchemy.orm import Session
from sqlalchemy import func, desc
from app.models.feedback import Feedback
from typing import Optional, List, Dict
import logging

logger = logging.getLogger(__name__)


class FeedbackRepository:
    """
    Repository for feedback database operations
    Implements upsert logic and query methods
    """
    
    def __init__(self, db: Session):
        self.db = db
    
    def create_or_update(
        self,
        product_id: int,
        user_id: int,
        rating: Optional[int] = None,
        comment: Optional[str] = None
    ) -> Feedback:
        """
        Upsert logic: Create new feedback or update existing one
        One feedback per user per product (enforced by UNIQUE constraint)
        
        Args:
            product_id: ID of the product
            user_id: ID of the user
            rating: Rating value (1-5) or None
            comment: Review comment or None
            
        Returns:
            Feedback: Created or updated feedback object
        """
        # Check if feedback already exists for this user-product combination
        existing_feedback = self.db.query(Feedback).filter(
            Feedback.product_id == product_id,
            Feedback.user_id == user_id
        ).first()
        
        if existing_feedback:
            # UPDATE existing feedback
            logger.info(f"Updating feedback {existing_feedback.id} for user {user_id} on product {product_id}")
            existing_feedback.rating = rating
            existing_feedback.comment = comment
            # updated_at will be automatically updated by onupdate trigger
            self.db.commit()
            self.db.refresh(existing_feedback)
            return existing_feedback
        else:
            # INSERT new feedback
            logger.info(f"Creating new feedback for user {user_id} on product {product_id}")
            new_feedback = Feedback(
                product_id=product_id,
                user_id=user_id,
                rating=rating,
                comment=comment
            )
            self.db.add(new_feedback)
            self.db.commit()
            self.db.refresh(new_feedback)
            return new_feedback
    
    def get_by_id(self, feedback_id: int) -> Optional[Feedback]:
        """
        Get feedback by ID
        
        Args:
            feedback_id: Feedback ID
            
        Returns:
            Feedback object or None if not found
        """
        return self.db.query(Feedback).filter(Feedback.id == feedback_id).first()
    
    def get_by_user_and_product(self, user_id: int, product_id: int) -> Optional[Feedback]:
        """
        Get feedback for specific user and product
        
        Args:
            user_id: User ID
            product_id: Product ID
            
        Returns:
            Feedback object or None if not found
        """
        return self.db.query(Feedback).filter(
            Feedback.user_id == user_id,
            Feedback.product_id == product_id
        ).first()
    
    def update(self, feedback: Feedback, rating: Optional[int], comment: Optional[str]) -> Feedback:
        """
        Update existing feedback
        
        Args:
            feedback: Feedback object to update
            rating: New rating value
            comment: New comment text
            
        Returns:
            Updated feedback object
        """
        feedback.rating = rating
        feedback.comment = comment
        self.db.commit()
        self.db.refresh(feedback)
        return feedback
    
    def delete(self, feedback: Feedback) -> None:
        """
        Delete feedback
        
        Args:
            feedback: Feedback object to delete
        """
        self.db.delete(feedback)
        self.db.commit()
    
    def get_by_product(
        self,
        product_id: int,
        skip: int = 0,
        limit: int = 20
    ) -> tuple[List[Feedback], int]:
        """
        Get all feedback for a product with pagination
        Sorted by newest first
        
        Args:
            product_id: Product ID
            skip: Number of records to skip
            limit: Maximum number of records to return
            
        Returns:
            Tuple of (list of feedback, total count)
        """
        query = self.db.query(Feedback).filter(Feedback.product_id == product_id)
        total = query.count()
        
        feedbacks = query.order_by(desc(Feedback.created_at)).offset(skip).limit(limit).all()
        
        return feedbacks, total
    
    def get_by_user(
        self,
        user_id: int,
        skip: int = 0,
        limit: int = 20
    ) -> tuple[List[Feedback], int]:
        """
        Get all feedback by a user with pagination
        Sorted by newest first
        
        Args:
            user_id: User ID
            skip: Number of records to skip
            limit: Maximum number of records to return
            
        Returns:
            Tuple of (list of feedback, total count)
        """
        query = self.db.query(Feedback).filter(Feedback.user_id == user_id)
        total = query.count()
        
        feedbacks = query.order_by(desc(Feedback.created_at)).offset(skip).limit(limit).all()
        
        return feedbacks, total
    
    def get_rating_summary(self, product_id: int) -> Dict:
        """
        Calculate rating statistics for a product
        
        Args:
            product_id: Product ID
            
        Returns:
            Dictionary with rating statistics
        """
        # Get all ratings for the product (excluding feedback with no rating)
        ratings = self.db.query(Feedback.rating).filter(
            Feedback.product_id == product_id,
            Feedback.rating.isnot(None)
        ).all()
        
        if not ratings:
            return {
                "product_id": product_id,
                "average_rating": 0.0,
                "total_ratings": 0,
                "rating_distribution": {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}
            }
        
        # Extract rating values
        rating_values = [r[0] for r in ratings]
        
        # Calculate average
        average_rating = round(sum(rating_values) / len(rating_values), 2)
        
        # Calculate distribution
        rating_distribution = {1: 0, 2: 0, 3: 0, 4: 0, 5: 0}
        for rating in rating_values:
            rating_distribution[rating] = rating_distribution.get(rating, 0) + 1
        
        return {
            "product_id": product_id,
            "average_rating": average_rating,
            "total_ratings": len(rating_values),
            "rating_distribution": rating_distribution
        }
