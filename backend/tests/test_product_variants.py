import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.models.product import Product
from app.models.product_variant import ProductVariant
from app.models.tenant import Tenant

@pytest.fixture
async def base_product(db: AsyncSession, test_tenant: Tenant) -> Product:
    product = Product(
        tenant_id=test_tenant.id,
        name="Camiseta Teste",
        price=50.0,
        is_active=True
    )
    db.add(product)
    await db.commit()
    await db.refresh(product)
    return product

@pytest.fixture
async def base_variant(db: AsyncSession, base_product: Product) -> ProductVariant:
    variant = ProductVariant(
        product_id=base_product.id,
        sku="TSHIRT-TEST-M",
        color="Blue",
        size="M",
        price_override=55.0,
        current_stock=10.0,
        is_active=True
    )
    db.add(variant)
    await db.commit()
    await db.refresh(variant)
    return variant

@pytest.mark.asyncio
async def test_create_product_variant(
    client: AsyncClient,
    owner_headers: dict,
    db: AsyncSession,
    base_product: Product
):
    payload = {
        "product_id": str(base_product.id),
        "sku": "TSHIRT-NEW-L",
        "color": "Red",
        "size": "L",
        "price_override": 60.0,
        "current_stock": 5.0,
        "minimum_stock": 2.0,
        "is_active": True
    }
    
    response = await client.post("/api/v1/product-variants/", headers=owner_headers, json=payload)
    assert response.status_code == 201
    
    data = response.json()
    assert data["sku"] == "TSHIRT-NEW-L"
    assert data["color"] == "Red"
    assert data["size"] == "L"
    assert data["product_id"] == str(base_product.id)
    assert data["price_override"] == 60.0
    
    # Check DB
    stmt = select(ProductVariant).where(ProductVariant.sku == "TSHIRT-NEW-L")
    result = await db.execute(stmt)
    variant_db = result.scalar_one_or_none()
    assert variant_db is not None
    assert variant_db.current_stock == 5.0

@pytest.mark.asyncio
async def test_create_duplicate_sku(
    client: AsyncClient,
    owner_headers: dict,
    base_product: Product,
    base_variant: ProductVariant
):
    payload = {
        "product_id": str(base_product.id),
        "sku": "TSHIRT-TEST-M", # duplicate
        "color": "Green",
    }
    
    response = await client.post("/api/v1/product-variants/", headers=owner_headers, json=payload)
    assert response.status_code == 400
    assert "já está em uso" in response.json()["detail"]

@pytest.mark.asyncio
async def test_list_variants_by_product(
    client: AsyncClient,
    owner_headers: dict,
    base_product: Product,
    base_variant: ProductVariant
):
    response = await client.get(f"/api/v1/product-variants/product/{base_product.id}", headers=owner_headers)
    assert response.status_code == 200
    
    data = response.json()
    assert len(data) >= 1
    assert any(v["id"] == str(base_variant.id) for v in data)

@pytest.mark.asyncio
async def test_update_product_variant(
    client: AsyncClient,
    owner_headers: dict,
    db: AsyncSession,
    base_variant: ProductVariant
):
    payload = {
        "price_override": 75.0,
        "current_stock": 20.0
    }
    
    response = await client.put(f"/api/v1/product-variants/{base_variant.id}", headers=owner_headers, json=payload)
    assert response.status_code == 200
    
    data = response.json()
    assert data["price_override"] == 75.0
    assert data["current_stock"] == 20.0
    
    # Reload from DB
    await db.refresh(base_variant)
    assert base_variant.price_override == 75.0
    assert base_variant.current_stock == 20.0

@pytest.mark.asyncio
async def test_delete_product_variant(
    client: AsyncClient,
    owner_headers: dict,
    db: AsyncSession,
    base_variant: ProductVariant
):
    response = await client.delete(f"/api/v1/product-variants/{base_variant.id}", headers=owner_headers)
    assert response.status_code == 204
    
    # Check DB
    stmt = select(ProductVariant).where(ProductVariant.id == base_variant.id)
    result = await db.execute(stmt)
    assert result.scalar_one_or_none() is None
