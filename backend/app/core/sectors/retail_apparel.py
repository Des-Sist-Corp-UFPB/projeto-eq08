"""
Estratégia para o setor retail_apparel (Loja de Roupas / Varejo de Moda).

Módulos ativos: products (com variantes futuras), categories, orders, suppliers, purchases
Módulos DESATIVADOS: insumos, product_recipes (sem ingredientes em loja de roupa)
KPIs: receita total, ticket médio, mix de vendas, giro de estoque
"""
from __future__ import annotations

import uuid

from sqlalchemy import func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.sectors.base import SectorStrategy
from app.models.order import Order, OrderItem
from app.models.product import Product
from app.models.supplier import Supplier
from app.models.category import Category


class RetailApparelStrategy(SectorStrategy):
    """
    Estratégia para lojas de roupas e varejo de moda.

    Desativa módulos de insumos/receitas (irrelevantes para vestuário).
    O contexto de BI foca em mix de vendas, giro de estoque e receita por categoria.

    Nota: O módulo ProductVariant (cor/tamanho/grade) está planejado para o Módulo B.
    """

    sector_type = "retail_apparel"

    def get_active_modules(self) -> list[str]:
        return [
            "products",
            "categories",
            # insumos: DESATIVADO — sem ingredientes em loja de roupas
            # product_recipes: DESATIVADO — sem receitas culinárias
            "orders",
            "suppliers",
            "purchases",
            "schedules",
            "ai_copilot",
            "audit",
        ]

    def get_kpi_definitions(self) -> dict[str, str]:
        return {
            "receita_total": "Soma total das vendas realizadas",
            "ticket_medio": "Valor médio por pedido/venda",
            "total_vendas": "Contagem total de vendas fechadas",
            "mix_categorias": "Distribuição de vendas por categoria de produto",
            "produtos_ativos": "Total de SKUs ativos no catálogo",
            "fornecedores_ativos": "Parceiros e marcas cadastradas",
        }

    def get_copilot_suggestions(self) -> list[str]:
        return [
            "Qual é a receita total de vendas?",
            "Quais são as categorias mais vendidas?",
            "Quantos produtos temos no catálogo?",
            "Quantos fornecedores/marcas temos cadastrados?",
            "Qual é o ticket médio das vendas?",
        ]

    async def build_bi_context(
        self,
        tenant_id: uuid.UUID,
        tenant_name: str,
        db: AsyncSession,
    ) -> str:
        """
        Constrói contexto de BI para loja de roupas/varejo.

        Consulta: receita, vendas, mix por categoria e fornecedores.
        """
        # --- Receita e vendas ---
        orders_result = await db.execute(
            select(
                func.sum(Order.total_price),
                func.count(Order.id),
            ).filter(Order.tenant_id == tenant_id)
        )
        revenue, orders_count = orders_result.first()
        revenue = float(revenue) if revenue else 0.0
        orders_count = int(orders_count) if orders_count else 0
        avg_ticket = round(revenue / orders_count, 2) if orders_count > 0 else 0.0

        # --- Produtos ativos ---
        products_result = await db.execute(
            select(func.count(Product.id)).filter(
                Product.tenant_id == tenant_id,
                Product.is_active == True,  # noqa: E712
            )
        )
        active_products = int(products_result.scalar() or 0)

        # --- Mix de vendas por categoria (top 5) ---
        category_mix_result = await db.execute(
            select(
                Category.name,
                func.sum(OrderItem.unit_price * OrderItem.quantity).label("revenue"),
                func.count(OrderItem.id).label("qty"),
            )
            .join(Product, OrderItem.product_id == Product.id)
            .outerjoin(Category, Product.category_id == Category.id)
            .join(Order, OrderItem.order_id == Order.id)
            .filter(Order.tenant_id == tenant_id)
            .group_by(Category.name)
            .order_by(func.sum(OrderItem.unit_price * OrderItem.quantity).desc())
            .limit(5)
        )
        top_categories = category_mix_result.all()

        # --- Fornecedores ---
        sup_result = await db.execute(
            select(func.count(Supplier.id)).filter(Supplier.tenant_id == tenant_id)
        )
        suppliers_count = int(sup_result.scalar() or 0)

        # --- Montar contexto textual ---
        lines = [
            f"Empresa: {tenant_name} [Setor: Loja de Roupas / Varejo]",
            "",
            "# Financeiro / Vendas",
            f"- Total de vendas: {orders_count}",
            f"- Receita total: R$ {revenue:,.2f}",
            f"- Ticket médio: R$ {avg_ticket:,.2f}",
            "",
            f"# Catálogo: {active_products} produto(s) ativo(s)",
            "",
            "# Mix de Vendas por Categoria (Top 5)",
        ]

        if top_categories:
            for cat_name, cat_revenue, cat_qty in top_categories:
                cat_name = cat_name or "Sem Categoria"
                lines.append(
                    f"  - {cat_name}: {cat_qty} unidade(s) | R$ {float(cat_revenue or 0):,.2f}"
                )
        else:
            lines.append("  - Nenhuma venda registrada ainda.")

        lines += [
            "",
            f"# Fornecedores/Marcas cadastradas: {suppliers_count}",
        ]

        return "\n".join(lines)
