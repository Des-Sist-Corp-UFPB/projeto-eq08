"""
Estratégia para o setor food_service (Restaurante / Pizzaria / Food Service).

Migração da lógica original de `app.core.gemini.build_business_context()` para
o padrão Strategy, tornando o copiloto configurável por setor.

Módulos ativos: products, insumos, orders, suppliers, schedules, purchases
KPIs: faturamento bruto, ticket médio, contagem de pedidos, CMV, estoque crítico
"""
from __future__ import annotations

import uuid
from datetime import date

from sqlalchemy import func
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.future import select

from app.core.sectors.base import SectorStrategy
from app.models.insumo import Insumo
from app.models.order import Order
from app.models.supplier import Supplier
from app.models.schedules import EmployeeSchedule
from app.models.user import User


class FoodServiceStrategy(SectorStrategy):
    """
    Estratégia para restaurantes, pizzarias e food service em geral.

    Ativa todos os módulos de insumos, receitas (ProductIngredient) e escalas.
    O contexto de BI foca em faturamento, CMV, rupturas de estoque e turnos.
    """

    sector_type = "food_service"

    def get_active_modules(self) -> list[str]:
        return [
            "products",
            "categories",
            "insumos",        # específico de food_service
            "product_recipes", # específico de food_service (ProductIngredient)
            "orders",
            "suppliers",
            "purchases",
            "schedules",
            "ai_copilot",
            "audit",
        ]

    def get_kpi_definitions(self) -> dict[str, str]:
        return {
            "faturamento_bruto": "Soma total de todos os pedidos fechados",
            "ticket_medio": "Valor médio por pedido realizado",
            "total_pedidos": "Contagem total de pedidos realizados",
            "insumos_criticos": "Insumos com estoque abaixo do mínimo de segurança",
            "fornecedores_ativos": "Parceiros cadastrados na plataforma",
            "colaboradores_hoje": "Colaboradores escalados para o dia atual",
        }

    def get_copilot_suggestions(self) -> list[str]:
        return [
            "Qual o faturamento bruto atual?",
            "Quais insumos estão críticos de estoque?",
            "Quem está escalado para trabalhar hoje?",
            "Quantos fornecedores temos cadastrados?",
            "Qual é o ticket médio dos pedidos?",
        ]

    async def build_bi_context(
        self,
        tenant_id: uuid.UUID,
        tenant_name: str,
        db: AsyncSession,
    ) -> str:
        """
        Constrói contexto de BI para food service.

        Consulta: faturamento, pedidos, insumos críticos, fornecedores e escalas de hoje.
        """
        # --- Faturamento e pedidos ---
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

        # --- Insumos críticos ---
        critical_result = await db.execute(
            select(Insumo).filter(
                Insumo.tenant_id == tenant_id,
                Insumo.current_stock < Insumo.minimum_stock,
            )
        )
        critical_insumos = critical_result.scalars().all()

        # --- Fornecedores ---
        sup_result = await db.execute(
            select(func.count(Supplier.id)).filter(Supplier.tenant_id == tenant_id)
        )
        suppliers_count = int(sup_result.scalar() or 0)

        # --- Escalas de hoje ---
        today = date.today()
        sched_result = await db.execute(
            select(EmployeeSchedule, User)
            .join(User, EmployeeSchedule.user_id == User.id)
            .filter(
                EmployeeSchedule.tenant_id == tenant_id,
                EmployeeSchedule.shift_date == today,
            )
        )
        today_schedules = [
            {
                "employee_name": user.name,
                "role": user.role,
                "start_time": sched.start_time,
                "end_time": sched.end_time,
            }
            for sched, user in sched_result.all()
        ]

        # --- Montar contexto textual ---
        lines = [
            f"Empresa: {tenant_name} [Setor: Restaurante / Food Service]",
            "",
            "# Financeiro / Vendas",
            f"- Total de pedidos: {orders_count}",
            f"- Faturamento bruto: R$ {revenue:,.2f}",
            f"- Ticket médio: R$ {avg_ticket:,.2f}",
            "",
            "# Estoque Crítico (insumos abaixo do mínimo de segurança)",
        ]

        if critical_insumos:
            for item in critical_insumos:
                lines.append(
                    f"  - {item.name}: estoque atual={item.current_stock:.1f} "
                    f"{item.unit} | mínimo={item.minimum_stock:.1f} {item.unit}"
                )
        else:
            lines.append("  - Nenhum insumo em situação crítica.")

        lines += [
            "",
            f"# Fornecedores cadastrados: {suppliers_count}",
            "",
            "# Escalas de hoje",
        ]

        if today_schedules:
            for s in today_schedules:
                lines.append(
                    f"  - {s['employee_name']} ({s['role']}): "
                    f"{s['start_time']} até {s['end_time']}"
                )
        else:
            lines.append("  - Nenhum colaborador escalado para hoje.")

        return "\n".join(lines)
