import uuid
from datetime import datetime
from pydantic import BaseModel, Field, EmailStr
from app.schemas.tenant import SectorType


class UserBase(BaseModel):
    name: str = Field(..., min_length=2, max_length=255)
    email: EmailStr = Field(..., description="E-mail único de acesso")
    role: str = Field("OPERATOR", description="Role/Perfil de acesso (RBAC)")


class UserCreate(UserBase):
    password: str = Field(..., min_length=6)


class UserUpdate(BaseModel):
    name: str | None = None
    email: EmailStr | None = None
    password: str | None = None
    role: str | None = None
    is_active: bool | None = None


class UserOut(UserBase):
    id: uuid.UUID
    tenant_id: uuid.UUID | None
    is_active: bool
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True


class UserWithSector(UserOut):
    """Extensão do UserOut que inclui o sector_type do Tenant.
    
    Usado exclusivamente no endpoint GET /users/me para que o frontend
    possa aplicar feature flags e personalização de UI sem chamada extra.
    """
    sector_type: str = "generic"  # fallback seguro se tenant não tiver setor


class UserLogin(BaseModel):
    email: EmailStr
    password: str


class UserRegisterTenant(BaseModel):
    company_name: str = Field(..., min_length=2, max_length=255)
    slug: str = Field(..., min_length=2, max_length=255)
    # sector_type define o setor de atuação para configurar módulos e IA
    sector_type: SectorType = Field(
        default="generic",
        description="Setor do negócio: food_service | retail_apparel | generic"
    )
    admin_name: str = Field(..., min_length=2, max_length=255)
    admin_email: EmailStr
    admin_password: str = Field(..., min_length=6)
