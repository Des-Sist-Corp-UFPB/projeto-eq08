"""
Pacote de estratégias por setor de negócio (SectorStrategy Pattern).

Para adicionar um novo setor:
1. Crie `backend/app/core/sectors/<nome_setor>.py` implementando `SectorStrategy`.
2. Registre a classe em `registry.py`.
3. Adicione o novo literal em `app/schemas/tenant.py > SectorType`.
"""
from app.core.sectors.base import SectorStrategy
from app.core.sectors.registry import SectorRegistry, get_strategy

__all__ = ["SectorStrategy", "SectorRegistry", "get_strategy"]
