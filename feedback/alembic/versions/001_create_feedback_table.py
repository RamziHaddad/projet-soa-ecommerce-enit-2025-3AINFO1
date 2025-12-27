"""Create feedback table with unified rating and comment support

Revision ID: 001
Revises: 
Create Date: 2024-01-15 10:00:00.000000

"""
from typing import Sequence, Union
from alembic import op
import sqlalchemy as sa
from sqlalchemy.sql import func

# revision identifiers, used by Alembic.
revision: str = '001'
down_revision: Union[str, None] = None
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    """
    Create the feedback table with:
    - Unified support for ratings and/or comments
    - Upsert-friendly unique constraint (one feedback per user per product)
    - Database-level validation constraints
    - Performance indexes
    """
    op.create_table(
        'feedback',
        # Primary key
        sa.Column('id', sa.BigInteger(), autoincrement=True, nullable=False),
        
        # Foreign keys (conceptually - actual FK constraints depend on your architecture)
        sa.Column('product_id', sa.BigInteger(), nullable=False),
        sa.Column('user_id', sa.BigInteger(), nullable=False),
        
        # Optional content fields (at least one required by constraint)
        sa.Column('rating', sa.Integer(), nullable=True),
        sa.Column('comment', sa.Text(), nullable=True),
        
        # Timestamps
        sa.Column('created_at', sa.TIMESTAMP(timezone=True), server_default=func.now(), nullable=False),
        sa.Column('updated_at', sa.TIMESTAMP(timezone=True), server_default=func.now(), nullable=False),
        
        # Primary key constraint
        sa.PrimaryKeyConstraint('id', name='pk_feedback'),
        
        # Unique constraint: one feedback per user per product (enables upsert)
        sa.UniqueConstraint('product_id', 'user_id', name='unique_user_product'),
        
        # Check constraints for data validation
        sa.CheckConstraint('rating >= 1 AND rating <= 5', name='valid_rating_range'),
        sa.CheckConstraint('LENGTH(comment) <= 2000', name='valid_comment_length'),
        sa.CheckConstraint('rating IS NOT NULL OR comment IS NOT NULL', name='at_least_one_field'),
    )
    
    # Create indexes for query performance
    op.create_index('idx_feedback_product_id', 'feedback', ['product_id'])
    op.create_index('idx_feedback_user_id', 'feedback', ['user_id'])
    op.create_index(
        'idx_feedback_created_at',
        'feedback',
        ['created_at'],
        postgresql_ops={'created_at': 'DESC'}
    )


def downgrade() -> None:
    """
    Drop the feedback table and all associated indexes
    """
    op.drop_index('idx_feedback_created_at', table_name='feedback')
    op.drop_index('idx_feedback_user_id', table_name='feedback')
    op.drop_index('idx_feedback_product_id', table_name='feedback')
    op.drop_table('feedback')
