"""
Testes para o pacote de estratégias de setor (SectorStrategy Pattern).

Cobertura:
- SectorRegistry: resolução correta por sector_type, fallback para unknown
- FoodServiceStrategy: módulos ativos, KPIs, build_bi_context
- RetailApparelStrategy: módulos ativos, KPIs, build_bi_context, ausência de insumos
- GenericStrategy: módulos ativos, KPIs, build_bi_context
- Integração: register-tenant com sector_type via endpoint HTTP
"""
import uuid
import pytest
import pytest_asyncio
from unittest.mock import AsyncMock, MagicMock, patch

from app.core.sectors import get_strategy, SectorRegistry
from app.core.sectors.food_service import FoodServiceStrategy
from app.core.sectors.retail_apparel import RetailApparelStrategy
from app.core.sectors.generic import GenericStrategy


# ─────────────────────────────────────────────
# Testes de SectorRegistry
# ─────────────────────────────────────────────

class TestSectorRegistry:
    def test_get_food_service_returns_correct_strategy(self):
        strategy = get_strategy("food_service")
        assert isinstance(strategy, FoodServiceStrategy)
        assert strategy.sector_type == "food_service"

    def test_get_retail_apparel_returns_correct_strategy(self):
        strategy = get_strategy("retail_apparel")
        assert isinstance(strategy, RetailApparelStrategy)
        assert strategy.sector_type == "retail_apparel"

    def test_get_generic_returns_correct_strategy(self):
        strategy = get_strategy("generic")
        assert isinstance(strategy, GenericStrategy)
        assert strategy.sector_type == "generic"

    def test_unknown_sector_returns_generic_fallback(self):
        """Setores desconhecidos devem retornar GenericStrategy sem exceção."""
        strategy = get_strategy("supermercado_hipotetico")
        assert isinstance(strategy, GenericStrategy)

    def test_list_sectors_contains_all_registered(self):
        sectors = SectorRegistry.list_sectors()
        assert "food_service" in sectors
        assert "retail_apparel" in sectors
        assert "generic" in sectors

    def test_registry_returns_singleton_instances(self):
        """Deve retornar a mesma instância (singleton stateless) em chamadas múltiplas."""
        s1 = get_strategy("food_service")
        s2 = get_strategy("food_service")
        assert s1 is s2


# ─────────────────────────────────────────────
# Testes de FoodServiceStrategy
# ─────────────────────────────────────────────

class TestFoodServiceStrategy:
    @pytest.fixture
    def strategy(self):
        return FoodServiceStrategy()

    def test_active_modules_contains_insumos(self, strategy):
        modules = strategy.get_active_modules()
        assert "insumos" in modules
        assert "product_recipes" in modules
        assert "orders" in modules
        assert "products" in modules
        assert "suppliers" in modules

    def test_kpi_definitions_not_empty(self, strategy):
        kpis = strategy.get_kpi_definitions()
        assert len(kpis) > 0
        assert "faturamento_bruto" in kpis
        assert "insumos_criticos" in kpis

    def test_copilot_suggestions_not_empty(self, strategy):
        suggestions = strategy.get_copilot_suggestions()
        assert len(suggestions) > 0
        assert all(isinstance(s, str) for s in suggestions)

    @pytest.mark.asyncio
    async def test_build_bi_context_returns_string_with_company_name(self, strategy):
        """build_bi_context deve retornar string com o nome da empresa."""
        tenant_id = uuid.uuid4()
        tenant_name = "Pizzaria Napolitana"

        # Mock da sessão do banco
        db = AsyncMock()

        # Mock para query de pedidos (revenue, count)
        orders_mock = MagicMock()
        orders_mock.first.return_value = (1500.50, 30)

        # Mock para query de insumos críticos
        critical_mock = MagicMock()
        critical_mock.scalars.return_value.all.return_value = []

        # Mock para query de fornecedores (count)
        sup_mock = MagicMock()
        sup_mock.scalar.return_value = 5

        # Mock para query de escalas
        sched_mock = MagicMock()
        sched_mock.all.return_value = []

        db.execute.side_effect = [
            orders_mock,
            critical_mock,
            sup_mock,
            sched_mock,
        ]

        context = await strategy.build_bi_context(tenant_id, tenant_name, db)

        # O Python usa o formato de número padrão (inglês): 1,500.50
        assert tenant_name in context
        assert "food_service" in context.lower() or "restaurante" in context.lower()
        assert "1,500.50" in context
        assert "Fornecedores" in context

    @pytest.mark.asyncio
    async def test_build_bi_context_with_critical_insumos(self, strategy):
        """Deve listar insumos críticos no contexto."""
        tenant_id = uuid.uuid4()
        tenant_name = "Restaurante Test"

        db = AsyncMock()

        orders_mock = MagicMock()
        orders_mock.first.return_value = (0.0, 0)

        insumo_mock = MagicMock()
        insumo_mock.name = "Farinha de Trigo"
        insumo_mock.current_stock = 2.0
        insumo_mock.minimum_stock = 10.0
        insumo_mock.unit = "kg"

        critical_mock = MagicMock()
        critical_mock.scalars.return_value.all.return_value = [insumo_mock]

        sup_mock = MagicMock()
        sup_mock.scalar.return_value = 0

        sched_mock = MagicMock()
        sched_mock.all.return_value = []

        db.execute.side_effect = [orders_mock, critical_mock, sup_mock, sched_mock]

        context = await strategy.build_bi_context(tenant_id, tenant_name, db)

        assert "Farinha de Trigo" in context
        assert "2.0" in context


# ─────────────────────────────────────────────
# Testes de RetailApparelStrategy
# ─────────────────────────────────────────────

class TestRetailApparelStrategy:
    @pytest.fixture
    def strategy(self):
        return RetailApparelStrategy()

    def test_active_modules_excludes_insumos(self, strategy):
        """Loja de roupas NÃO deve ter módulo de insumos ativo."""
        modules = strategy.get_active_modules()
        assert "insumos" not in modules
        assert "product_recipes" not in modules

    def test_active_modules_contains_core_modules(self, strategy):
        modules = strategy.get_active_modules()
        assert "products" in modules
        assert "orders" in modules
        assert "suppliers" in modules

    def test_kpi_definitions_contains_retail_kpis(self, strategy):
        kpis = strategy.get_kpi_definitions()
        assert "mix_categorias" in kpis
        assert "produtos_ativos" in kpis

    def test_copilot_suggestions_are_retail_specific(self, strategy):
        suggestions = strategy.get_copilot_suggestions()
        assert len(suggestions) > 0
        # Verifica que sugestões não mencionam insumos
        suggestions_text = " ".join(suggestions).lower()
        assert "insumo" not in suggestions_text

    @pytest.mark.asyncio
    async def test_build_bi_context_returns_retail_context(self, strategy):
        """Contexto deve mencionar 'Loja de Roupas' e não 'Food Service'."""
        tenant_id = uuid.uuid4()
        tenant_name = "Boutique Elegance"

        db = AsyncMock()

        # Pedidos
        orders_mock = MagicMock()
        orders_mock.first.return_value = (8500.0, 120)

        # Produtos ativos
        products_mock = MagicMock()
        products_mock.scalar.return_value = 45

        # Mix de categorias (top 5)
        cat_mock = MagicMock()
        cat_mock.all.return_value = [
            ("Camisetas", 3000.0, 60),
            ("Calças", 2500.0, 40),
        ]

        # Fornecedores
        sup_mock = MagicMock()
        sup_mock.scalar.return_value = 8

        db.execute.side_effect = [orders_mock, products_mock, cat_mock, sup_mock]

        context = await strategy.build_bi_context(tenant_id, tenant_name, db)

        assert tenant_name in context
        assert "Loja de Roupas" in context or "Varejo" in context
        assert "Camisetas" in context
        assert "Calças" in context


# ─────────────────────────────────────────────
# Testes de GenericStrategy
# ─────────────────────────────────────────────

class TestGenericStrategy:
    @pytest.fixture
    def strategy(self):
        return GenericStrategy()

    def test_active_modules_are_universal(self, strategy):
        modules = strategy.get_active_modules()
        assert "products" in modules
        assert "orders" in modules
        assert "suppliers" in modules
        # Não ativa módulos específicos de setores
        assert "insumos" not in modules

    def test_kpi_definitions_exist(self, strategy):
        kpis = strategy.get_kpi_definitions()
        assert len(kpis) > 0
        assert "receita_total" in kpis

    @pytest.mark.asyncio
    async def test_build_bi_context_returns_generic_context(self, strategy):
        tenant_id = uuid.uuid4()
        tenant_name = "Empresa Genérica"

        db = AsyncMock()

        orders_mock = MagicMock()
        orders_mock.first.return_value = (500.0, 10)

        sup_mock = MagicMock()
        sup_mock.scalar.return_value = 3

        db.execute.side_effect = [orders_mock, sup_mock]

        context = await strategy.build_bi_context(tenant_id, tenant_name, db)

        assert tenant_name in context
        assert "Negócio Genérico" in context
        assert "500" in context


# ─────────────────────────────────────────────
# Testes de Integração: Register-tenant com sector_type
# ─────────────────────────────────────────────

@pytest.mark.asyncio
async def test_register_tenant_with_food_service_sector(client):
    """Deve registrar tenant com sector_type food_service e persistir o valor."""
    payload = {
        "company_name": "Pizzaria Bella Napoli",
        "slug": "pizzaria-bella-napoli",
        "sector_type": "food_service",
        "admin_name": "Antonio Rossi",
        "admin_email": "antonio@bellanapoli.com",
        "admin_password": "senhasegura123",
    }
    response = await client.post("/api/v1/auth/register-tenant", json=payload)
    assert response.status_code == 201


@pytest.mark.asyncio
async def test_register_tenant_with_retail_apparel_sector(client):
    """Deve registrar tenant com sector_type retail_apparel."""
    payload = {
        "company_name": "Boutique Elegance",
        "slug": "boutique-elegance",
        "sector_type": "retail_apparel",
        "admin_name": "Maria Lucia",
        "admin_email": "maria@elegance.com",
        "admin_password": "senhasegura123",
    }
    response = await client.post("/api/v1/auth/register-tenant", json=payload)
    assert response.status_code == 201


@pytest.mark.asyncio
async def test_register_tenant_defaults_to_generic_sector(client):
    """Sem sector_type explícito, deve usar 'generic' como padrão."""
    payload = {
        "company_name": "Empresa Sem Setor",
        "slug": "empresa-sem-setor",
        # sector_type omitido — deve usar default "generic"
        "admin_name": "João Silva",
        "admin_email": "joao@setor.com",
        "admin_password": "senhasegura123",
    }
    response = await client.post("/api/v1/auth/register-tenant", json=payload)
    assert response.status_code == 201


@pytest.mark.asyncio
async def test_register_tenant_rejects_invalid_sector(client):
    """sector_type inválido deve retornar HTTP 422 (Unprocessable Entity)."""
    payload = {
        "company_name": "Empresa Inválida",
        "slug": "empresa-invalida",
        "sector_type": "setor_inexistente",  # ← inválido
        "admin_name": "Teste Inválido",
        "admin_email": "invalido@teste.com",
        "admin_password": "senhasegura123",
    }
    response = await client.post("/api/v1/auth/register-tenant", json=payload)
    assert response.status_code == 422

