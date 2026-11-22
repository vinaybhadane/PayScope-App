"""Application configuration settings."""
from typing import List, Union
import json
from pydantic import ValidationInfo, field_validator, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """Core settings for PayScope API."""
    
    PROJECT_NAME: str = "PayScope API"
    VERSION: str = "0.1.0"
    API_V1_STR: str = "/api/v1"
    ENVIRONMENT: str = "development"
    RATE_LIMIT_STORAGE_URI: str = "memory://"
    
    FIREBASE_PROJECT_ID: str = "payscope18"
    API_SESSION_SECRET: str = ""
    ALLOW_GUEST_SESSIONS: bool = True
    RUN_MIGRATIONS: bool = True
    GEMINI_API_KEY: str = ""
    GEMINI_MODEL: str = "gemini-3.5-flash-lite"
    GEMINI_FALLBACK_MODELS: str = ""
    BREVO_API_KEY: str = ""
    BREVO_SENDER_EMAIL: str = "patilsakshi18027@gmail.com"
    EMAIL_REMINDER_INTERVAL_SECONDS: int = 3600
    EMAIL_WORKER_ENABLED: bool = True

    # Server configuration
    API_HOST: str = "0.0.0.0"
    API_PORT: int = 8000
    
    # CORS settings - development defaults and production origins
    BACKEND_CORS_ORIGINS: Union[List[str], str] = [
        "http://localhost:3000",
        "http://127.0.0.1:3000",
        "http://localhost:3001",
        "http://127.0.0.1:3001",
        "https://pay-scope-lake.vercel.app",
        "https://payscope.vercel.app",
    ]

    @field_validator("BACKEND_CORS_ORIGINS", mode="before")
    @classmethod
    def assemble_cors_origins(cls, v: Union[str, List[str]]) -> List[str]:
        parsed_origins: List[str] = []
        if isinstance(v, str):
            v_str = v.strip()
            if v_str.startswith("[") and v_str.endswith("]"):
                try:
                    parsed = json.loads(v_str)
                    if isinstance(parsed, list):
                        parsed_origins = [str(i).strip().rstrip("/") for i in parsed if str(i).strip()]
                except Exception:
                    parsed_origins = [i.strip().rstrip("/") for i in v_str.strip("[]").split(",") if i.strip()]
            else:
                parsed_origins = [i.strip().rstrip("/") for i in v_str.split(",") if i.strip()]
        elif isinstance(v, list):
            parsed_origins = [str(i).strip().rstrip("/") for i in v if str(i).strip()]
        
        all_origins = parsed_origins
        seen = set()
        deduped: List[str] = []
        for origin in all_origins:
            if origin == "*" or "*" in origin:
                raise ValueError("Credentialed CORS requires explicit origins; wildcards are forbidden")
            if origin and origin not in seen:
                seen.add(origin)
                deduped.append(origin)
        
        return deduped
    
    # Database Configuration (PostgreSQL 16 / Neon)
    POSTGRES_DB: str = "mandatelens"
    POSTGRES_USER: str = "mandatelens_dev"
    POSTGRES_PASSWORD: str = "mandatelens_dev_password"

    @field_validator("POSTGRES_PASSWORD")
    @classmethod
    def validate_production_password(cls, value: str, info: ValidationInfo) -> str:
        if info.data.get("ENVIRONMENT", "development").lower() == "production" and (
            not value or value in {"mandatelens_dev_password", "CHANGE_ME", "CHANGE_ME_LOCAL_ONLY"}
        ):
            raise ValueError("Production requires POSTGRES_PASSWORD from an environment variable or secret manager")
        return value

    DATABASE_URL: str = "postgresql+psycopg://mandatelens_dev:mandatelens_dev_password@127.0.0.1:5432/mandatelens"
    TEST_DATABASE_URL: str = "postgresql+psycopg://mandatelens_dev:mandatelens_dev_password@127.0.0.1:5432/mandatelens_test"

    @field_validator("DATABASE_URL", mode="before")
    @classmethod
    def assemble_db_connection(cls, v: str) -> str:
        if not v:
            return v
        # Detect unconfigured placeholder URLs (such as example 'ep-xyz.neon.tech' or 'CHANGE_ME')
        if "ep-xyz.neon.tech" in v or "CHANGE_ME" in v:
            return "postgresql+psycopg://mandatelens_dev:mandatelens_dev_password@127.0.0.1:5432/mandatelens"
        if v.startswith("postgres://"):
            return v.replace("postgres://", "postgresql+psycopg://", 1)
        elif v.startswith("postgresql://") and not v.startswith("postgresql+psycopg://"):
            return v.replace("postgresql://", "postgresql+psycopg://", 1)
        return v

    @model_validator(mode="after")
    def validate_production_guest_sessions(self):
        if self.ENVIRONMENT.lower() == "production" and self.ALLOW_GUEST_SESSIONS:
            if len(self.API_SESSION_SECRET.strip()) < 32 or self.API_SESSION_SECRET.startswith("payscope-secure-session-key-fallback"):
                raise ValueError("Production guest sessions require a unique API_SESSION_SECRET of at least 32 characters")
        return self

    @model_validator(mode="after")
    def validate_production_database(self):
        if self.ENVIRONMENT.lower() == "production":
            from sqlalchemy.engine import make_url
            if make_url(self.DATABASE_URL).password in {None, "", "mandatelens_dev_password", "CHANGE_ME", "CHANGE_ME_LOCAL_ONLY"}:
                raise ValueError("Production DATABASE_URL must contain non-default database credentials")
        return self
    
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        case_sensitive=True,
        extra="ignore",
        hide_input_in_errors=True,
    )


settings = Settings()
