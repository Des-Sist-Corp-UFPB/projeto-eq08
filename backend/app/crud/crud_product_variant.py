import uuid
from typing import Sequence
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.models.product_variant import ProductVariant
from app.schemas.product_variant import ProductVariantCreate, ProductVariantUpdate


async def create_product_variant(
    db: AsyncSession, obj_in: ProductVariantCreate
) -> ProductVariant:
    db_obj = ProductVariant(**obj_in.model_dump())
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj


async def get_product_variant_by_id(
    db: AsyncSession, variant_id: uuid.UUID
) -> ProductVariant | None:
    stmt = select(ProductVariant).where(ProductVariant.id == variant_id)
    result = await db.execute(stmt)
    return result.scalar_one_or_none()


async def get_product_variant_by_sku(
    db: AsyncSession, sku: str
) -> ProductVariant | None:
    stmt = select(ProductVariant).where(ProductVariant.sku == sku)
    result = await db.execute(stmt)
    return result.scalar_one_or_none()


async def get_product_variants_by_product(
    db: AsyncSession, product_id: uuid.UUID
) -> Sequence[ProductVariant]:
    stmt = select(ProductVariant).where(ProductVariant.product_id == product_id)
    result = await db.execute(stmt)
    return result.scalars().all()


async def update_product_variant(
    db: AsyncSession, db_obj: ProductVariant, obj_in: ProductVariantUpdate
) -> ProductVariant:
    update_data = obj_in.model_dump(exclude_unset=True)
    for field, value in update_data.items():
        setattr(db_obj, field, value)
    db.add(db_obj)
    await db.commit()
    await db.refresh(db_obj)
    return db_obj


async def delete_product_variant(
    db: AsyncSession, db_obj: ProductVariant
) -> None:
    await db.delete(db_obj)
    await db.commit()
