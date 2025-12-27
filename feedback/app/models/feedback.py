"""
Database models for Feedback Service
"""
from sqlalchemy import Column, BigInteger, Integer, Text, TIMESTAMP, CheckConstraint, UniqueConstraint, Index
from sqlalchemy.sql import func
from app.database import Base


class Feedback(Base):
    """
    Unified Feedback model that stores both ratings and comments
    Supports:
    - Rating only (rating provided, comment is NULL)
    - Comment only (comment provided, rating is NULL)
    - Full review (both rating and comment provided)
    """
    __tablename__ = "feedback"
    
    id = Column(BigInteger, primary_key=True, index=True, autoincrement=True)
    product_id = Column(BigInteger, nullable=False, index=True)
    user_id = Column(BigInteger, nullable=False, index=True)
    
    # Optional fields - at least one must be provided
    rating = Column(Integer, nullable=True)  # 1-5 stars
    comment = Column(Text, nullable=True)    # Up to 2000 characters
    
    created_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), nullable=False)
    updated_at = Column(TIMESTAMP(timezone=True), server_default=func.now(), onupdate=func.now(), nullable=False)
    
    # Constraints
    __table_args__ = (
        # Ensure one rating per user per product (enables upsert logic)
        UniqueConstraint('product_id', 'user_id', name='unique_user_product'),
        
        # Rating must be between 1 and 5
        CheckConstraint('rating >= 1 AND rating <= 5', name='valid_rating_range'),
        
        # Comment must not exceed 2000 characters
        CheckConstraint('LENGTH(comment) <= 2000', name='valid_comment_length'),
        
        # At least one field (rating or comment) must be provided
        CheckConstraint('rating IS NOT NULL OR comment IS NOT NULL', name='at_least_one_field'),
        
        # Index for sorting by creation date (newest first)
        Index('idx_feedback_created_at', 'created_at', postgresql_ops={'created_at': 'DESC'}),
    )
    
    def __repr__(self):
        return f"<Feedback(id={self.id}, product_id={self.product_id}, user_id={self.user_id}, rating={self.rating})>"
