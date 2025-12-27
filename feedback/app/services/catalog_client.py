"""
External service client for Catalog Service
Implements resilience patterns (retry, timeout)
"""
import httpx
from tenacity import (
    retry,
    stop_after_attempt,
    wait_exponential,
    retry_if_exception_type
)
from app.config import settings
import logging

logger = logging.getLogger(__name__)


class CatalogServiceClient:
    """
    Client for interacting with the Catalog Service
    Implements retry logic with exponential backoff
    """
    
    def __init__(self):
        self.base_url = settings.CATALOG_SERVICE_URL
        self.timeout = settings.REQUEST_TIMEOUT
    
    @retry(
        stop=stop_after_attempt(settings.MAX_RETRY_ATTEMPTS),
        wait=wait_exponential(
            multiplier=1,
            min=settings.RETRY_WAIT_MIN,
            max=settings.RETRY_WAIT_MAX
        ),
        retry=retry_if_exception_type((httpx.RequestError, httpx.TimeoutException)),
        reraise=True
    )
    async def check_product_exists(self, product_id: int) -> bool:
        """
        Check if a product exists in the catalog service
        Implements retry logic with exponential backoff
        
        Args:
            product_id: Product ID to check
            
        Returns:
            bool: True if product exists, False otherwise
            
        Raises:
            httpx.RequestError: If all retry attempts fail
            httpx.TimeoutException: If request times out after retries
        """
        url = f"{self.base_url}/products/{product_id}"
        
        try:
            async with httpx.AsyncClient() as client:
                logger.info(f"Checking if product {product_id} exists in catalog service")
                response = await client.get(url, timeout=self.timeout)
                
                if response.status_code == 200:
                    logger.info(f"Product {product_id} exists in catalog")
                    return True
                elif response.status_code == 404:
                    logger.warning(f"Product {product_id} not found in catalog")
                    return False
                else:
                    # Unexpected status code - will trigger retry
                    logger.warning(f"Unexpected status {response.status_code} from catalog service")
                    response.raise_for_status()
                    return False
                    
        except httpx.TimeoutException as e:
            logger.error(f"Timeout while checking product {product_id}: {e}")
            raise
        except httpx.RequestError as e:
            logger.error(f"Request error while checking product {product_id}: {e}")
            raise
    
    async def get_product(self, product_id: int) -> dict:
        """
        Get product details from catalog service
        
        Args:
            product_id: Product ID
            
        Returns:
            dict: Product data
            
        Raises:
            httpx.RequestError: If request fails
        """
        url = f"{self.base_url}/products/{product_id}"
        
        async with httpx.AsyncClient() as client:
            response = await client.get(url, timeout=self.timeout)
            response.raise_for_status()
            return response.json()


# Create singleton instance
catalog_client = CatalogServiceClient()
