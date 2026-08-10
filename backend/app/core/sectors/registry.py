"""
Registro central de estratégias de setor (SectorRegistry).

Mapeia o valor de `tenant.sector_type` para a Strategy correta.
O fallback padrão é GenericStrategy se o setor não for reconhecido.

Uso:
    strategy = get_strategy(tenant.sector_type)
    context = await strategy.build_bi_context(tenant.id, tenant.name, db)
"""
from __future__ import annotations

import logging

from app.core.sectors.base import SectorStrategy
from app.core.sectors.food_service import FoodServiceStrategy
from app.core.sectors.retail_apparel import RetailApparelStrategy
from app.core.sectors.generic import GenericStrategy

logger = logging.getLogger(__name__)

# Instâncias singleton por setor (stateless — nenhuma depende de estado de request)
_REGISTRY: dict[str, SectorStrategy] = {
    FoodServiceStrategy.sector_type: FoodServiceStrategy(),
    RetailApparelStrategy.sector_type: RetailApparelStrategy(),
    GenericStrategy.sector_type: GenericStrategy(),
}

_FALLBACK: SectorStrategy = GenericStrategy()


class SectorRegistry:
    """
    Registry estático de estratégias.
    Todas as strategies são singletons sem estado.
    """

    @staticmethod
    def get(sector_type: str) -> SectorStrategy:
        """
        Retorna a SectorStrategy para o setor informado.
        Se o setor não for reconhecido, retorna GenericStrategy com um aviso de log.
        """
        strategy = _REGISTRY.get(sector_type)
        if strategy is None:
            logger.warning(
                "Setor '%s' não possui Strategy registrada — usando GenericStrategy como fallback.",
                sector_type,
            )
            return _FALLBACK
        return strategy

    @staticmethod
    def list_sectors() -> list[str]:
        """Retorna todos os setores registrados."""
        return list(_REGISTRY.keys())


def get_strategy(sector_type: str) -> SectorStrategy:
    """
    Atalho de conveniência para `SectorRegistry.get()`.

    Preferir esta função nos endpoints e serviços para reduzir verbosidade.
    """
    return SectorRegistry.get(sector_type)
