"""
Classe base abstrata para estratégias de setor (SectorStrategy).

Cada setor de negócio suportado pela plataforma deve implementar esta interface.
Isso garante que todos os setores forneçam:
  - Um contexto estruturado para o copiloto de IA (BIaaS)
  - A lista de módulos ativos naquele setor
  - Definições de KPIs relevantes
"""
from __future__ import annotations

import uuid
from abc import ABC, abstractmethod

from sqlalchemy.ext.asyncio import AsyncSession


class SectorStrategy(ABC):
    """
    Interface que cada setor de negócio deve implementar.

    Cada subclasse é responsável por:
    - Construir o contexto de BI específico para o prompt de IA.
    - Declarar quais módulos do sistema estão ativos no setor.
    - Definir os KPIs monitorados pelo setor.
    """

    #: Identificador único do setor (deve corresponder ao valor em SectorType)
    sector_type: str

    @abstractmethod
    async def build_bi_context(self, tenant_id: uuid.UUID, tenant_name: str, db: AsyncSession) -> str:
        """
        Constrói a string de contexto estruturado para injeção no prompt do copiloto.

        Args:
            tenant_id: UUID do tenant ativo.
            tenant_name: Nome da empresa para personalização do contexto.
            db: Sessão assíncrona do SQLAlchemy.

        Returns:
            String formatada com os dados reais do negócio.
        """
        ...

    @abstractmethod
    def get_active_modules(self) -> list[str]:
        """
        Retorna a lista de identificadores de módulos ativos neste setor.

        Usado pelo frontend para habilitar/desabilitar rotas e componentes de UI.
        Exemplo: ["products", "orders", "suppliers", "insumos", "schedules"]
        """
        ...

    @abstractmethod
    def get_kpi_definitions(self) -> dict[str, str]:
        """
        Retorna dicionário com os KPIs do setor e suas descrições.

        Usado pelo dashboard de BI para exibir métricas relevantes.
        Exemplo: {"ticket_medio": "Valor médio por pedido/venda"}
        """
        ...

    def get_copilot_suggestions(self) -> list[str]:
        """
        Retorna sugestões de perguntas para o copiloto de IA, contextualizado ao setor.
        Pode ser sobrescrito pelas subclasses para customizar a UX do chat.
        """
        return [
            "Qual é o faturamento atual?",
            "Quais fornecedores temos cadastrados?",
            "Quem está escalado para hoje?",
        ]
