"""
Redis cache utilities
"""
import redis.asyncio as redis
import json
from typing import Optional, Any
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class RedisCache:
    """
    Redis cache wrapper with async support
    """
    
    def __init__(self):
        self.redis_url = settings.REDIS_URL
        self.ttl = settings.CACHE_TTL_SECONDS
        self._client: Optional[redis.Redis] = None
    
    async def get_client(self) -> redis.Redis:
        """
        Get or create Redis client
        """
        if self._client is None:
            self._client = redis.from_url(
                self.redis_url,
                encoding="utf-8",
                decode_responses=True
            )
        return self._client
    
    async def ping(self) -> bool:
        """
        Test Redis connection
        """
        try:
            client = await self.get_client()
            await client.ping()
            return True
        except Exception as e:
            logger.error(f"Redis ping failed: {e}")
            return False
    
    async def close(self):
        """
        Close Redis connection
        """
        if self._client:
            await self._client.close()


# Create singleton instance
redis_client = RedisCache()


async def cache_get(key: str) -> Optional[dict]:
    """
    Get value from cache
    
    Args:
        key: Cache key
        
    Returns:
        Cached data or None if not found
    """
    try:
        client = await redis_client.get_client()
        data = await client.get(key)
        
        if data:
            return json.loads(data)
        return None
        
    except Exception as e:
        logger.error(f"Cache get error for key {key}: {e}")
        return None


async def cache_set(key: str, value: Any, ttl: Optional[int] = None) -> bool:
    """
    Set value in cache
    
    Args:
        key: Cache key
        value: Value to cache (will be JSON serialized)
        ttl: Time to live in seconds (defaults to settings.CACHE_TTL_SECONDS)
        
    Returns:
        True if successful, False otherwise
    """
    try:
        client = await redis_client.get_client()
        ttl = ttl or settings.CACHE_TTL_SECONDS
        
        serialized = json.dumps(value)
        await client.setex(key, ttl, serialized)
        
        return True
        
    except Exception as e:
        logger.error(f"Cache set error for key {key}: {e}")
        return False


async def cache_delete(key: str) -> bool:
    """
    Delete value from cache
    
    Args:
        key: Cache key
        
    Returns:
        True if successful, False otherwise
    """
    try:
        client = await redis_client.get_client()
        await client.delete(key)
        
        return True
        
    except Exception as e:
        logger.error(f"Cache delete error for key {key}: {e}")
        return False


async def cache_clear_pattern(pattern: str) -> int:
    """
    Clear all keys matching a pattern
    
    Args:
        pattern: Redis key pattern (e.g., "rating_summary:*")
        
    Returns:
        Number of keys deleted
    """
    try:
        client = await redis_client.get_client()
        keys = []
        
        async for key in client.scan_iter(match=pattern):
            keys.append(key)
        
        if keys:
            deleted = await client.delete(*keys)
            return deleted
        
        return 0
        
    except Exception as e:
        logger.error(f"Cache clear pattern error for {pattern}: {e}")
        return 0
