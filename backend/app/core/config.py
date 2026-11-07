from __future__ import annotations

from cryptography.fernet import Fernet
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    environment: str = Field(default="development", alias="ENVIRONMENT")
    api_v1_prefix: str = Field(default="/api/v1", alias="API_V1_PREFIX")
    postgres_dsn: str = Field(
        default="postgresql+asyncpg://engbuddy:engbuddy_dev_pass@localhost:5432/engbuddy",
        alias="POSTGRES_DSN",
    )
    redis_url: str = Field(default="redis://localhost:6379/0", alias="REDIS_URL")

    jwt_secret_key: str = Field(
        default="CHANGE_ME_GENERATE_WITH_OPENSSL_RAND_HEX_32",
        alias="JWT_SECRET_KEY",
    )
    jwt_algorithm: str = "HS256"
    access_token_expire_minutes: int = 60 * 24 * 7

    oauth_token_encryption_key: str = Field(
        default="",
        alias="OAUTH_TOKEN_ENCRYPTION_KEY",
    )

    s3_endpoint_url: str = Field(default="http://localhost:9000", alias="S3_ENDPOINT_URL")
    s3_access_key_id: str = Field(default="minioadmin", alias="S3_ACCESS_KEY_ID")
    s3_secret_access_key: str = Field(default="minioadmin", alias="S3_SECRET_ACCESS_KEY")
    s3_bucket_name: str = Field(default="engbuddy-vault", alias="S3_BUCKET_NAME")
    s3_region: str = Field(default="auto", alias="S3_REGION")

    google_oauth_client_id: str = Field(default="", alias="GOOGLE_OAUTH_CLIENT_ID")
    google_oauth_client_secret: str = Field(default="", alias="GOOGLE_OAUTH_CLIENT_SECRET")
    google_oauth_redirect_uri: str = Field(
        default="http://localhost:8000/api/v1/integrations/google/callback",
        alias="GOOGLE_OAUTH_REDIRECT_URI",
    )

    gemini_api_key: str = Field(default="", alias="GEMINI_API_KEY")

    enable_whatsapp_cloud_webhook: bool = Field(
        default=False,
        alias="ENABLE_WHATSAPP_CLOUD_WEBHOOK",
    )
    whatsapp_verify_token: str = Field(default="", alias="WHATSAPP_VERIFY_TOKEN")
    whatsapp_access_token: str = Field(default="", alias="WHATSAPP_ACCESS_TOKEN")

    allowed_cors_origins: list[str] = [
        "http://localhost:3000",
        "http://localhost:8080",
        "https://app.engbuddy.io",
    ]

    _cached_fernet: Fernet | None = None

    def get_fernet(self) -> Fernet:
        if self._cached_fernet is not None:
            return self._cached_fernet
        key = self.oauth_token_encryption_key.strip() if self.oauth_token_encryption_key else ""
        if not key or key.startswith("CHANGE_ME"):
            if self.environment == "production":
                raise ValueError(
                    "OAUTH_TOKEN_ENCRYPTION_KEY must be configured with a 32-byte url-safe base64 key in production"
                )
            # Deterministic dev fallback key (32 bytes url-safe base64) to prevent data loss across restarts
            key = "ZGV2X2tleV9mb3JfZW5nYnVkZHlfYXBwXzMyYnl0ZXM="
        try:
            self._cached_fernet = Fernet(key.encode("utf-8"))
        except Exception as e:
            if self.environment == "production":
                raise ValueError(f"Invalid OAUTH_TOKEN_ENCRYPTION_KEY: {e}") from e
            # Deterministic valid fallback key for development
            self._cached_fernet = Fernet(b"ZGV2X2tleV9mb3JfZW5nYnVkZHlfYXBwXzMyYnl0ZXM=")
        return self._cached_fernet


settings = Settings()
