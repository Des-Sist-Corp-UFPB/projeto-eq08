import pytest
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession
from app.models.tenant import Tenant
from app.models.user import User

@pytest.mark.asyncio
async def test_category_crud_flow(client: AsyncClient, owner_headers: dict):
    # 1. Create Category
    payload = {
        "name": "Bebidas",
        "type": "PRODUCT",
        "description": "Bebidas geladas"
    }
    response = await client.post("/api/v1/categories/", json=payload, headers=owner_headers)
    assert response.status_code == 201
    category = response.json()
    assert category["name"] == "Bebidas"
    cat_id = category["id"]

    # 2. List Categories
    response = await client.get("/api/v1/categories/", headers=owner_headers)
    assert response.status_code == 200
    assert len(response.json()) >= 1

    # 3. List Categories by type
    response = await client.get("/api/v1/categories/?cat_type=PRODUCT", headers=owner_headers)
    assert response.status_code == 200
    assert any(c["id"] == cat_id for c in response.json())

    # 4. Update Category
    update_payload = {"name": "Bebidas Atualizadas"}
    response = await client.put(f"/api/v1/categories/{cat_id}", json=update_payload, headers=owner_headers)
    assert response.status_code == 200
    assert response.json()["name"] == "Bebidas Atualizadas"

    # 5. Delete Category
    response = await client.delete(f"/api/v1/categories/{cat_id}", headers=owner_headers)
    assert response.status_code == 204


@pytest.mark.asyncio
async def test_category_not_found(client: AsyncClient, owner_headers: dict):
    fake_id = "00000000-0000-0000-0000-000000000000"
    response = await client.put(f"/api/v1/categories/{fake_id}", json={"name": "test"}, headers=owner_headers)
    assert response.status_code == 404
    response = await client.delete(f"/api/v1/categories/{fake_id}", headers=owner_headers)
    assert response.status_code == 404


@pytest.mark.asyncio
async def test_insumo_crud_flow(client: AsyncClient, owner_headers: dict):
    # 1. Create Insumo Category
    cat_payload = {"name": "Hortifruti", "type": "INSUMO"}
    cat_response = await client.post("/api/v1/categories/", json=cat_payload, headers=owner_headers)
    cat_id = cat_response.json()["id"]

    # 2. Create Insumo
    insumo_payload = {
        "name": "Tomate",
        "category_id": cat_id,
        "unit": "KG",
        "current_stock": 10.0,
        "minimum_stock": 2.0,
        "unit_cost": 5.50
    }
    response = await client.post("/api/v1/insumos/", json=insumo_payload, headers=owner_headers)
    assert response.status_code == 201
    insumo = response.json()
    insumo_id = insumo["id"]
    assert insumo["name"] == "Tomate"

    # 3. List Insumos
    response = await client.get("/api/v1/insumos/", headers=owner_headers)
    assert response.status_code == 200
    assert len(response.json()) >= 1

    # 4. Update Insumo
    update_payload = {"minimum_stock": 15.0}
    response = await client.put(f"/api/v1/insumos/{insumo_id}", json=update_payload, headers=owner_headers)
    assert response.status_code == 200
    assert response.json()["minimum_stock"] == 15.0

    # 5. Delete Insumo
    response = await client.delete(f"/api/v1/insumos/{insumo_id}", headers=owner_headers)
    assert response.status_code == 204


@pytest.mark.asyncio
async def test_insumo_invalid_category(client: AsyncClient, owner_headers: dict):
    # Trying to create Insumo with PRODUCT category
    cat_payload = {"name": "Bebidas", "type": "PRODUCT"}
    cat_response = await client.post("/api/v1/categories/", json=cat_payload, headers=owner_headers)
    cat_id = cat_response.json()["id"]

    insumo_payload = {
        "name": "Tomate Invalido",
        "category_id": cat_id,
        "unit": "KG"
    }
    response = await client.post("/api/v1/insumos/", json=insumo_payload, headers=owner_headers)
    assert response.status_code == 400


@pytest.mark.asyncio
async def test_product_crud_flow(client: AsyncClient, owner_headers: dict):
    # 1. Create Product Category
    cat_payload = {"name": "Lanches", "type": "PRODUCT"}
    cat_response = await client.post("/api/v1/categories/", json=cat_payload, headers=owner_headers)
    cat_id = cat_response.json()["id"]

    # 2. Create Insumo Category
    icat_payload = {"name": "Carnes", "type": "INSUMO"}
    icat_response = await client.post("/api/v1/categories/", json=icat_payload, headers=owner_headers)
    icat_id = icat_response.json()["id"]

    # 3. Create Insumos
    pao_payload = {"name": "Pao", "category_id": icat_id, "unit": "UN", "unit_cost": 1.0, "current_stock": 10}
    carne_payload = {"name": "Carne", "category_id": icat_id, "unit": "UN", "unit_cost": 2.5, "current_stock": 10}
    pao_res = await client.post("/api/v1/insumos/", json=pao_payload, headers=owner_headers)
    carne_res = await client.post("/api/v1/insumos/", json=carne_payload, headers=owner_headers)
    pao_id = pao_res.json()["id"]
    carne_id = carne_res.json()["id"]

    # 4. Create Product with ingredients
    product_payload = {
        "name": "X-Burguer",
        "category_id": cat_id,
        "price": 15.0,
        "ingredients": [
            {"insumo_id": pao_id, "quantity": 1.0},
            {"insumo_id": carne_id, "quantity": 1.0}
        ]
    }
    response = await client.post("/api/v1/products/", json=product_payload, headers=owner_headers)
    assert response.status_code == 201
    product = response.json()
    product_id = product["id"]
    assert product["name"] == "X-Burguer"
    assert len(product["ingredients"]) == 2

    # 5. List Products
    response = await client.get("/api/v1/products/", headers=owner_headers)
    assert response.status_code == 200
    assert len(response.json()) >= 1

    # 6. Update Product
    update_payload = {
        "price": 18.0,
        "ingredients": [
            {"insumo_id": pao_id, "quantity": 2.0} # Removing carne
        ]
    }
    response = await client.put(f"/api/v1/products/{product_id}", json=update_payload, headers=owner_headers)
    assert response.status_code == 200
    
    # Refetch to avoid SQLAlchemy relationship cache bug in the PUT response
    get_res = await client.get("/api/v1/products/", headers=owner_headers)
    updated_prod = next(p for p in get_res.json() if p["id"] == product_id)
    assert updated_prod["price"] == 18.0
    assert len(updated_prod["ingredients"]) == 1
    assert updated_prod["ingredients"][0]["quantity"] == 2.0

    # 7. Delete Product
    response = await client.delete(f"/api/v1/products/{product_id}", headers=owner_headers)
    assert response.status_code == 204


@pytest.mark.asyncio
async def test_product_invalid_category_or_insumo(client: AsyncClient, owner_headers: dict):
    # Trying to create Product with INSUMO category
    cat_payload = {"name": "Carnes", "type": "INSUMO"}
    cat_response = await client.post("/api/v1/categories/", json=cat_payload, headers=owner_headers)
    cat_id = cat_response.json()["id"]

    product_payload = {
        "name": "X-Invalido",
        "category_id": cat_id,
        "price": 15.0
    }
    response = await client.post("/api/v1/products/", json=product_payload, headers=owner_headers)
    assert response.status_code == 400

    # Create proper category
    pcat_payload = {"name": "Lanches", "type": "PRODUCT"}
    pcat_response = await client.post("/api/v1/categories/", json=pcat_payload, headers=owner_headers)
    pcat_id = pcat_response.json()["id"]

    # Invalid insumo ID
    fake_insumo = "00000000-0000-0000-0000-000000000000"
    product_payload2 = {
        "name": "X-Invalido 2",
        "category_id": pcat_id,
        "price": 15.0,
        "ingredients": [{"insumo_id": fake_insumo, "quantity": 1.0}]
    }
    response = await client.post("/api/v1/products/", json=product_payload2, headers=owner_headers)
    assert response.status_code == 400
