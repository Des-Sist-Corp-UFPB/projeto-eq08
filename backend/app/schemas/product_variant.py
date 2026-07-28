import uuid
from datetime import datetime
from pydantic import BaseModel, Field


class ProductVariantBase(BaseModel):
    sku: str = Field(..., min_length=2, max_length=100)
    color: str | None = None
    size: str | None = None
    price_override: float | None = Field(None, ge=0)
    current_stock: float = Field(0.0, ge=0)
    minimum_stock: float = Field(0.0, ge=0)
    is_active: bool = True


class ProductVariantCreate(ProductVariantBase):
    product_id: uuid.UUID


class ProductVariantUpdate(BaseModel):
    sku: str | None = Field(None, min_length=2, max_length=100)
    color: str | None = None
    size: str | None = None
    price_override: float | None = Field(None, ge=0)
    current_stock: float | None = Field(None, ge=0)
    minimum_stock: float | None = Field(None, ge=0)
    is_active: bool | None = None


class ProductVariantOut(ProductVariantBase):
    id: uuid.UUID
    product_id: uuid.UUID
    created_at: datetime
    updated_at: datetime

    class Config:
        from_attributes = True
