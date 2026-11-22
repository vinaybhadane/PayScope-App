"""Environment configuration for the derived Neo4j projection."""

from pathlib import Path

from pydantic import AliasChoices, Field
from pydantic_settings import BaseSettings, SettingsConfigDict

_root = Path(__file__).resolve().parents[4]


class Neo4jSettings(BaseSettings):
    ENABLED: bool = False
    REQUIRED: bool = False
    URI: str = ""
    USERNAME: str = Field(default="", validation_alias=AliasChoices("NEO4J_USER", "NEO4J_USERNAME"))
    PASSWORD: str = Field(default="", repr=False)
    DATABASE: str = "neo4j"
    MAX_CONNECTION_LIFETIME: int = 3600
    MAX_CONNECTION_POOL_SIZE: int = 50
    CONNECTION_TIMEOUT: float = 3.0
    CONNECTION_ACQUISITION_TIMEOUT: float = 4.0
    QUERY_TIMEOUT: float = 4.0
    SYNC_QUERY_TIMEOUT: float = 30.0
    SYNC_BATCH_SIZE: int = 500

    model_config = SettingsConfigDict(
        env_prefix="NEO4J_",
        env_file=(_root / ".env", _root / "apps/api/.env"),
        env_file_encoding="utf-8",
        extra="ignore",
        hide_input_in_errors=True,
    )


neo4j_settings = Neo4jSettings()
