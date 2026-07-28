import uuid
from typing import Annotated, List
from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from app.api import deps
from app.core.database import get_db
from app.crud.crud_product import get_product_by_id
from app.crud.crud_product_variant import (
    create_product_variant, get_product_variant_by_id, get_product_variant_by_sku,
    get_product_variants_by_product, update_product_variant, delete_product_variant
)
from app.models.tenant import Tenant
from app.models.user import User
from app.schemas.product_variant import ProductVariantCreate, ProductVariantUpdate, ProductVariantOut

router = APIRouter()


@router.get("/product/{product_id}", response_model=List[ProductVariantOut])
async def list_variants_by_product(
    product_id: uuid.UUID,
    db: Annotated[AsyncSession, Depends(get_db)],
    current_tenant: Annotated[Tenant, Depends(deps.get_current_tenant)],
    current_user: Annotated[User, Depends(deps.get_current_tenant_user)]
):
    """
    List all variants for a specific product.
    """
    product = await get_product_by_id(db, product_id)
    if not product or product.tenant_id != current_tenant.id:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Produto não encontrado."
        )
    variants = await get_product_variants_by_product(db, product_id)
    return variants


@router.post("/", response_model=ProductVariantOut, status_code=status.HTTP_201_CREATED)
async def create_new_variant(
    obj_in: ProductVariantCreate,
    db: Annotated[AsyncSession, Depends(get_db)],
    current_tenant: Annotated[Tenant, Depends(deps.get_current_tenant)],
    current_user: Annotated[User, Depends(deps.get_current_tenant_user)],
    _: Annotated[User, Depends(deps.RoleChecker(["OWNER", "MANAGER"]))]
):
    """
    Create a new product variant. Restricted to OWNER or MANAGER.
    """
    product = await get_product_by_id(db, obj_in.product_id)
    if not product or product.tenant_id != current_tenant.id:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Produto base não encontrado."
        )

    existing_sku = await get_product_variant_by_sku(db, obj_in.sku)
    if existing_sku:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="O SKU informado já está em uso."
        )

    variant = await create_product_variant(db, obj_in)
    return variant


@router.put("/{variant_id}", response_model=ProductVariantOut)
async def update_existing_variant(
    variant_id: uuid.UUID,
    obj_in: ProductVariantUpdate,
    db: Annotated[AsyncSession, Depends(get_db)],
    current_tenant: Annotated[Tenant, Depends(deps.get_current_tenant)],
    current_user: Annotated[User, Depends(deps.get_current_tenant_user)],
    _: Annotated[User, Depends(deps.RoleChecker(["OWNER", "MANAGER"]))]
):
    """
    Update a product variant. Restricted to OWNER or MANAGER.
    """
    variant = await get_product_variant_by_id(db, variant_id)
    if not variant:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Variante não encontrada."
        )
        
    product = await get_product_by_id(db, variant.product_id)
    if not product or product.tenant_id != current_tenant.id:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Variante não encontrada ou não pertence ao seu tenant."
        )

    if obj_in.sku and obj_in.sku != variant.sku:
        existing_sku = await get_product_variant_by_sku(db, obj_in.sku)
        if existing_sku:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="O SKU informado já está em uso."
            )

    updated = await update_product_variant(db, variant, obj_in)
    return updated


@router.delete("/{variant_id}", status_code=status.HTTP_204_NO_CONTENT)
async def delete_existing_variant(
    variant_id: uuid.UUID,
    db: Annotated[AsyncSession, Depends(get_db)],
    current_tenant: Annotated[Tenant, Depends(deps.get_current_tenant)],
    current_user: Annotated[User, Depends(deps.get_current_tenant_user)],
    _: Annotated[User, Depends(deps.RoleChecker(["OWNER", "MANAGER"]))]
):
    """
    Delete a product variant. Restricted to OWNER or MANAGER.
    """
    variant = await get_product_variant_by_id(db, variant_id)
    if not variant:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Variante não encontrada."
        )
        
    product = await get_product_by_id(db, variant.product_id)
    if not product or product.tenant_id != current_tenant.id:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Variante não encontrada."
        )

    await delete_product_variant(db, variant)
