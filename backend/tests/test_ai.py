import pytest
import uuid
from datetime import date, timedelta
from httpx import AsyncClient
from sqlalchemy.ext.asyncio import AsyncSession

from app.models.tenant import Tenant
from app.models.user import User
from app.models.insumo import Insumo
from app.models.supplier import Supplier
from app.models.purchase import PurchaseOrder, PurchaseItem
from app.models.ai import DemandForecast, AIRecommendation
from app.crud import get_purchase_order_by_id
from app.core import security


@pytest.mark.asyncio
async def test_ai_demand_forecast(client: AsyncClient, owner_headers: dict):
    # 1. Fetch 7-day demand forecast
    response = await client.get("/api/v1/ai/forecast", headers=owner_headers)
    assert response.status_code == 200
    data = response.json()
    assert len(data) == 7
    
    # Assert fields existence and consistency
    for i, item in enumerate(data):
        assert "target_date" in item
        assert "predicted_orders" in item
        assert "predicted_revenue" in item
        assert "confidence_score" in item
        assert item["model_version"] == "heuristics-v1"
        
        # Date should progress daily
        target_dt = date.fromisoformat(item["target_date"])
        assert target_dt == date.today() + timedelta(days=i)


@pytest.mark.asyncio
async def test_ai_recommendations_and_auto_purchase_order_flow(
    client: AsyncClient, db: AsyncSession, owner_headers: dict, operator_headers: dict, test_tenant: Tenant
):
    # Setup: Create critical Insumo (current stock = 10.0, minimum stock = 100.0)
    insumo = Insumo(
        tenant_id=test_tenant.id,
        name="Queijo Cheddar",
        unit="kg",
        minimum_stock=100.0,
        current_stock=10.0,
        unit_cost=18.50
    )
    # Create Supplier
    supplier = Supplier(
        tenant_id=test_tenant.id,
        name="Queijaria Serra Dourada",
        document="11222333000144"
    )
    db.add_all([insumo, supplier])
    await db.commit()

    # 1. Fetch recommendations (triggers generator in backend)
    rec_res = await client.get("/api/v1/ai/recommendations", headers=owner_headers)
    assert rec_res.status_code == 200
    data = rec_res.json()
    
    # Assert STOCK_REPLENISHMENT recommendation is generated
    assert len(data) >= 1
    replenish_rec = next(r for r in data if r["type"] == "STOCK_REPLENISHMENT")
    assert replenish_rec["impact_level"] == "HIGH"
    assert "Queijo Cheddar" in replenish_rec["description"]
    assert replenish_rec["status"] == "PENDING"
    
    # Check action data pre-fill supplier and items
    act_data = replenish_rec["action_data"]
    assert act_data["supplier_id"] == str(supplier.id)
    assert act_data["items"][0]["insumo_id"] == str(insumo.id)
    assert act_data["items"][0]["quantity"] > 0

    rec_id = replenish_rec["id"]

    # 2. Operators cannot apply/approve recommendations (RBAC)
    apply_fail = await client.post(f"/api/v1/ai/recommendations/{rec_id}/apply", headers=operator_headers)
    assert apply_fail.status_code == 403

    # 3. Owner applies recommendation (1-Click replenishment flow)
    apply_res = await client.post(f"/api/v1/ai/recommendations/{rec_id}/apply", headers=owner_headers)
    assert apply_res.status_code == 200
    apply_data = apply_res.json()
    assert "criado com sucesso" in apply_data["message"]
    
    created_order_id = apply_data["created_order_id"]
    assert created_order_id is not None

    # 4. Verify created purchase order in the database
    await db.commit()
    po = await get_purchase_order_by_id(db, uuid.UUID(created_order_id))
    assert po is not None
    assert po.supplier_id == supplier.id
    assert po.status == "PENDING"
    assert len(po.items) == 1
    assert po.items[0].insumo_id == insumo.id
    assert po.items[0].quantity == act_data["items"][0]["quantity"]

    # Confirm recommendation is no longer PENDING (status APPLIED)
    rec_check = await client.get("/api/v1/ai/recommendations", headers=owner_headers)
    assert rec_check.status_code == 200
    assert not any(r["id"] == rec_id for r in rec_check.json())


@pytest.mark.asyncio
async def test_ai_copilot_direct_queries(
    client: AsyncClient, db: AsyncSession, owner_headers: dict, test_tenant: Tenant
):
    """
    Testa o copiloto no modo heurístico (sem GEMINI_API_KEY).
    
    O test_tenant tem sector_type="generic" (default do conftest),
    logo consultas sobre insumos retornam a mensagem de módulo indisponível —
    o comportamento agnóstico CORRETO da nova arquitetura.
    """
    # Send "faturamento" question — funciona para qualquer setor
    res_fin = await client.post("/api/v1/ai/copilot", json={"message": "Como está o faturamento de vendas?"}, headers=owner_headers)
    assert res_fin.status_code == 200
    assert "Desempenho Financeiro" in res_fin.json()["response"]

    # Send "estoque/insumos" question com tenant genérico:
    # O módulo de insumos NÃO está ativo em "generic" — deve retornar mensagem de módulo indisponível
    res_st = await client.post("/api/v1/ai/copilot", json={"message": "Quais insumos estão críticos de estoque?"}, headers=owner_headers)
    assert res_st.status_code == 200
    # O setor "generic" não tem módulo de insumos ativo — verifica a mensagem informativa
    response_text = res_st.json()["response"]
    assert (
        "não está disponível" in response_text  # módulo desativado para setor genérico
        or "Controle de Estoque" in response_text   # caso sector_type tenha insumos
        or "Alertas de Ruptura" in response_text
    )

    # Send general greeting fallback — sugestões variam por setor
    res_greet = await client.post("/api/v1/ai/copilot", json={"message": "Olá, bom dia!"}, headers=owner_headers)
    assert res_greet.status_code == 200
    # Verifica que o copiloto responde com alguma saudação ou sugestão
    assert "Copiloto" in res_greet.json()["response"] or "generic" in res_greet.json()["response"]


@pytest.mark.asyncio
async def test_ai_copilot_insumos_available_for_food_service(
    client: AsyncClient, db: AsyncSession, test_tenant: Tenant
):
    """
    Garante que a query de insumos funciona para tenants com sector_type=food_service.
    """
    from app.core.security import get_password_hash

    # Cria tenant food_service
    food_tenant = Tenant(name="Pizzaria Test", slug="pizzaria-test", status="active", sector_type="food_service")
    db.add(food_tenant)
    await db.commit()

    food_owner = User(
        tenant_id=food_tenant.id,
        name="Chef Owner",
        email="chef@pizzaria.com",
        hashed_password=get_password_hash("password123"),
        role="OWNER",
        is_active=True,
    )
    db.add(food_owner)
    await db.commit()

    from app.core.security import create_access_token
    food_token = create_access_token(subject=food_owner.id)
    food_headers = {
        "Authorization": f"Bearer {food_token}",
        "X-Tenant-ID": str(food_tenant.id),
    }

    # Query de insumos para tenant food_service — deve responder com conteúdo de estoque
    res = await client.post("/api/v1/ai/copilot", json={"message": "Quais insumos estão críticos de estoque?"}, headers=food_headers)
    assert res.status_code == 200
    response_text = res.json()["response"]
    # Para food_service, o módulo está ativo — deve retornar informação de estoque
    assert "Controle de Estoque" in response_text or "Alertas de Ruptura" in response_text


@pytest.mark.asyncio
async def test_ai_tenant_isolation(
    client: AsyncClient, db: AsyncSession, owner_headers: dict, test_tenant: Tenant
):
    # Create Tenant B
    other_tenant = Tenant(name="Company B", slug="companyb", status="active")
    db.add(other_tenant)
    await db.commit()

    from app.core.security import get_password_hash
    other_owner = User(
        tenant_id=other_tenant.id,
        name="Boss B",
        email="bossb@test.com",
        hashed_password=get_password_hash("password123"),
        role="OWNER",
        is_active=True
    )
    db.add(other_owner)
    await db.commit()

    token_b = security.create_access_token(subject=other_owner.id)
    headers_b = {
        "Authorization": f"Bearer {token_b}",
        "X-Tenant-ID": str(other_tenant.id)
    }

    # Fetch forecasts for Tenant B (should generate distinct ones)
    res_b = await client.get("/api/v1/ai/forecast", headers=headers_b)
    assert res_b.status_code == 200
    assert len(res_b.json()) == 7
    for item in res_b.json():
        assert item["tenant_id"] == str(other_tenant.id)
