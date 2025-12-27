"""
Application Configuration using Pydantic Settings
"""
from pydantic_settings import BaseSettings
from pydantic import model_validator
from typing import List, Any


class Settings(BaseSettings):
    """
    Application settings loaded from environment variables
    """
    # Database
    DATABASE_URL: str = "postgresql://feedback_user:feedback_pass@localhost:5432/feedback_db"
    
    # Redis
    REDIS_URL: str = "redis://localhost:6379/0"
    
    # JWT
    JWT_SECRET_KEY: str = "your-super-secret-jwt-key-change-this-in-production"
    JWT_ALGORITHM: str = "HS256"
    JWT_ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    
    # External Services
    CATALOG_SERVICE_URL: str = "http://catalog-service:8000/api"
    
    # Application
    APP_NAME: str = "Feedback Service"
    APP_VERSION: str = "1.0.0"
    DEBUG: bool = False
    ALLOWED_ORIGINS: List[str] = ["http://localhost:3000", "http://localhost:8080"]
    
    # Cache
    CACHE_TTL_SECONDS: int = 3600
    
    # Retry Configuration
    MAX_RETRY_ATTEMPTS: int = 3
    RETRY_WAIT_MIN: int = 1
    RETRY_WAIT_MAX: int = 4
    REQUEST_TIMEOUT: float = 2.0
    
    @model_validator(mode='before')
    @classmethod
    def parse_cors_origins(cls, data: Any) -> Any:
        """Parse comma-separated ALLOWED_ORIGINS string to list"""
        if isinstance(data, dict):
            origins = data.get('ALLOWED_ORIGINS', data.get('allowed_origins'))
            if isinstance(origins, str):
                data['ALLOWED_ORIGINS'] = [origin.strip() for origin in origins.split(',')]
        return data
    
    class Config:
        env_file = ".env"
        case_sensitive = True


# Create global settings instance
settings = Settings()
