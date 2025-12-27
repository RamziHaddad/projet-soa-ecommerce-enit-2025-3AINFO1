"""
JWT Authentication middleware
"""
from fastapi import Depends, HTTPException, status
from fastapi.security import HTTPBearer, HTTPAuthorizationCredentials
from typing import Optional, Dict
from app.utils.security import extract_user_from_token
import logging

logger = logging.getLogger(__name__)

# HTTP Bearer token scheme
security = HTTPBearer()


async def get_current_user(
    credentials: HTTPAuthorizationCredentials = Depends(security)
) -> Dict:
    """
    Dependency to extract and validate current user from JWT token
    
    Args:
        credentials: HTTP Authorization credentials
        
    Returns:
        Dictionary with user information
        
    Raises:
        HTTPException: If token is invalid or user not found
    """
    token = credentials.credentials
    
    user_info = extract_user_from_token(token)
    
    if not user_info:
        logger.warning("Invalid or expired token")
        raise HTTPException(
            status_code=status.HTTP_401_UNAUTHORIZED,
            detail="Invalid or expired token",
            headers={"WWW-Authenticate": "Bearer"},
        )
    
    return user_info


async def get_current_user_optional(
    credentials: Optional[HTTPAuthorizationCredentials] = Depends(HTTPBearer(auto_error=False))
) -> Optional[Dict]:
    """
    Optional authentication - returns user if token provided, None otherwise
    Useful for endpoints that work both with and without authentication
    
    Args:
        credentials: HTTP Authorization credentials (optional)
        
    Returns:
        Dictionary with user information or None
    """
    if not credentials:
        return None
    
    token = credentials.credentials
    return extract_user_from_token(token)
