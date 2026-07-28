import uuid
from datetime import datetime
from typing import Literal
from pydantic import BaseModel, Field

# Literal com todos os setores suportados.
# Para adicionar um novo setor: inclua o valor aqui e crie a SectorStrategy correspondente.
SectorType = Literal["food_service", "retail_apparel", "generic"]

SECTOR_LABELS: dict[str, str] = {
    "food_service": "Restaurante / Food Service",
    "retail_apparel": "Loja de Roupas / Varejo",
    "generic": "Negócio Genérico",
}


class TenantBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=255, description="Nome da empresa/tenant")
    slug: str = Field(..., min_length=2, max_length=255, description="Slug identificador único")
    sector_type: SectorType = Field(
        default="generic",
        description="Setor de atuação do negócio. Define módulos ativos e contexto do copiloto de IA."
    )


class TenantCreate(TenantBase):
    pass


class TenantUpdate(BaseModel):
    name: str | None = None
    status: str | None = None
    sector_type: SectorType | None = None


class TenantOut(TenantBase):
    id: uuid.UUID
    status: str
    sector_type: SectorType
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
        json_schema_extra = {
            "example": {
                "id": "123e4567-e89b-12d3-a456-426614174000",
                "name": "Minha Lanchonete",
                "slug": "minha-lanchonete",
                "status": "active",
                "sector_type": "food_service",
                "created_at": "2026-06-02T03:30:00Z",
                "updated_at": "2026-06-02T03:30:00Z"
            }
        }
