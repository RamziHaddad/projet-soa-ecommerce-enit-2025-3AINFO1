"""
JWT security utilities
"""
from datetime import datetime, timedelta
from typing import Optional, Dict
from jose import JWTError, jwt
from app.config import settings
import logging

logger = logging.getLogger(__name__)


def create_access_token(data: dict, expires_delta: Optional[timedelta] = None) -> str:
    """
    Create JWT access token
    
    Args:
        data: Data to encode in token
        expires_delta: Token expiration time
        
    Returns:
        Encoded JWT token
    """
    to_encode = data.copy()
    
    if expires_delta:
        expire = datetime.utcnow() + expires_delta
    else:
        expire = datetime.utcnow() + timedelta(minutes=settings.JWT_ACCESS_TOKEN_EXPIRE_MINUTES)
    
    to_encode.update({"exp": expire})
    
    encoded_jwt = jwt.encode(
        to_encode,
        settings.JWT_SECRET_KEY,
        algorithm=settings.JWT_ALGORITHM
    )
    
    return encoded_jwt


def decode_access_token(token: str) -> Optional[Dict]:
    """
    Decode and validate JWT token
    
    Args:
        token: JWT token string
        
    Returns:
        Decoded payload or None if invalid
    """
    try:
        payload = jwt.decode(
            token,
            settings.JWT_SECRET_KEY,
            algorithms=[settings.JWT_ALGORITHM]
        )
        return payload
        
    except JWTError as e:
        logger.warning(f"JWT decode error: {e}")
        return None


def extract_user_from_token(token: str) -> Optional[Dict]:
    """
    Extract user information from JWT token
    
    Args:
        token: JWT token string
        
    Returns:
        Dictionary with user_id, username, and roles or None if invalid
    """
    payload = decode_access_token(token)
    
    if not payload:
        return None
    
    user_id = payload.get("user_id")
    username = payload.get("username")
    roles = payload.get("roles", [])
    
    if not user_id:
        return None
    
    return {
        "user_id": user_id,
        "username": username,
        "roles": roles
    }
