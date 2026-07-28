"""
Estratégia genérica (fallback) para setores ainda não especializados.

Fornece apenas o contexto básico de vendas e fornecedores, sem
especificidades de domínio. Serve como fallback seguro para qualquer
tenant cujo setor não tenha uma Strategy dedicada.
"""
from __future__ import annotations

import uuid

from sqlalchemy import func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.sectors.base import SectorStrategy
from app.models.order import Order
from app.models.supplier import Supplier


class GenericStrategy(SectorStrategy):
    """
    Estratégia fallback para o setor 'generic'.

    Ativa apenas os módulos universais (vendas, fornecedores, escalas).
    O contexto de BI é minimalista, cobrindo apenas faturamento básico.
    """

    sector_type = "generic"

    def get_active_modules(self) -> list[str]:
        return [
            "products",
            "categories",
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
            "ticket_medio": "Valor médio por transação",
            "total_vendas": "Contagem total de vendas",
            "fornecedores_ativos": "Parceiros cadastrados na plataforma",
        }

    def get_copilot_suggestions(self) -> list[str]:
        return [
            "Qual é o faturamento atual?",
            "Quantos fornecedores temos cadastrados?",
            "Quem está escalado para trabalhar hoje?",
        ]

    async def build_bi_context(
        self,
        tenant_id: uuid.UUID,
        tenant_name: str,
        db: AsyncSession,
    ) -> str:
        """
        Contexto de BI genérico: apenas faturamento e fornecedores.
        """
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

        sup_result = await db.execute(
            select(func.count(Supplier.id)).filter(Supplier.tenant_id == tenant_id)
        )
        suppliers_count = int(sup_result.scalar() or 0)

        lines = [
            f"Empresa: {tenant_name} [Setor: Negócio Genérico]",
            "",
            "# Financeiro / Vendas",
            f"- Total de vendas: {orders_count}",
            f"- Receita total: R$ {revenue:,.2f}",
            f"- Ticket médio: R$ {avg_ticket:,.2f}",
            "",
            f"# Fornecedores cadastrados: {suppliers_count}",
        ]

        return "\n".join(lines)
