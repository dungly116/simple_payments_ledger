from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    """Application settings from environment variables"""

    # Database
    database_url: str = "postgresql://postgres:postgres@localhost:5432/payments_ledger"
    db_host: str = "localhost"
    db_port: int = 5432
    db_name: str = "payments_ledger"
    db_user: str = "postgres"
    db_password: str = "postgres"

    # Application
    app_env: str = "development"
    debug: bool = True

    # Server
    host: str = "0.0.0.0"
    port: int = 8000

    # API
    api_key: str = "your-api-key-here"

    class Config:
        env_file = "app/.env"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    """Get cached settings instance"""
    return Settings()
