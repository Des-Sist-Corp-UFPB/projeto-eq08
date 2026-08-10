from typing import List, Optional
from pydantic import model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    API_V1_STR: str = "/api/v1"
    PROJECT_NAME: str = "Gestor de Negócio SaaS"

    # JWT Security — nomes alinhados com o env do Portainer
    JWT_SECRET: str                      # env: JWT_SECRET
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    JWT_EXPIRY_DAYS: int = 7             # env: JWT_EXPIRY_DAYS

    # Aliases internos para compatibilidade com security.py
    @property
    def SECRET_KEY(self) -> str:
        return self.JWT_SECRET

    @property
    def REFRESH_TOKEN_EXPIRE_DAYS(self) -> int:
        return self.JWT_EXPIRY_DAYS

    # ─── Banco de dados — lidas do ambiente (Portainer) ───
    # Mapeamento das propriedades Spring do servidor:
    #   spring.datasource.url      → DB_HOST, DB_PORT, DB_NAME
    #   spring.datasource.username → DB_USER
    #   spring.datasource.password → DB_PASSWORD
    #   hikari.maximum-pool-size   → DB_POOL_SIZE
    DB_HOST: str = "postgres"
    DB_PORT: int = 5432
    DB_NAME: str = "eq08"
    DB_USER: str
    DB_PASSWORD: str
    DB_POOL_SIZE: int = 10

    # Montada automaticamente a partir das variáveis acima.
    # Pode ser sobrescrita definindo DATABASE_URL diretamente no Portainer.
    DATABASE_URL: Optional[str] = None

    @model_validator(mode="after")
    def assemble_database_url(self) -> "Settings":
        """Monta DATABASE_URL asyncpg a partir das partes individuais."""
        if not self.DATABASE_URL:
            self.DATABASE_URL = (
                f"postgresql+asyncpg://{self.DB_USER}:{self.DB_PASSWORD}"
                f"@{self.DB_HOST}:{self.DB_PORT}/{self.DB_NAME}"
            )
        return self

    # Gemini AI
    GEMINI_API_KEY: Optional[str] = None

    # Redis
    REDIS_URL: str = "redis://localhost:6379"
    USE_REDIS: bool = False

    # CORS
    BACKEND_CORS_ORIGINS: List[str] = [
        "http://localhost:3000",
        "http://localhost:5173",
        "http://127.0.0.1:3000",
        "http://127.0.0.1:5173",
        "https://gestor-negocio.vercel.app"
    ]

    model_config = SettingsConfigDict(
        env_file=".env",
        case_sensitive=True,
        extra="allow"
    )


settings = Settings()
