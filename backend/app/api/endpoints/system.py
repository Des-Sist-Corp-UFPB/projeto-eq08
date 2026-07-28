from fastapi import APIRouter, HTTPException
from pydantic import BaseModel
import traceback
import sys
import os

# Ensure the backend root is in sys.path so we can import seed_db
backend_path = os.path.dirname(os.path.dirname(os.path.dirname(__file__)))
if backend_path not in sys.path:
    sys.path.insert(0, backend_path)

from seed_db import seed_data
from app.core.sectors import SectorRegistry
from app.schemas.tenant import SECTOR_LABELS

router = APIRouter()


class SeedResponse(BaseModel):
    message: str


class SectorOut(BaseModel):
    sector_type: str
    label: str


@router.post("/seed", response_model=SeedResponse)
async def seed_database():
    """
    Roda a função seed_data para popular o banco de dados.
    """
    try:
        await seed_data()
        return {"message": "Banco populado com sucesso!"}
    except Exception as e:
        error_details = traceback.format_exc()
        print(f"Unexpected error seeding DB:\n{error_details}")
        from fastapi.responses import JSONResponse
        return JSONResponse(status_code=400, content={"detail": f"Erro interno: {str(e)} | Trace: {error_details}", "traceback": error_details})


@router.get("/sectors", response_model=list[SectorOut])
async def list_available_sectors():
    """
    Retorna todos os setores de negócio disponíveis na plataforma.

    Usado pelo frontend para popular o select de setor no onboarding de tenant.
    Não requer autenticação — é informação pública de configuração.
    """
    sectors = SectorRegistry.list_sectors()
    return [
        SectorOut(
            sector_type=s,
            label=SECTOR_LABELS.get(s, s.replace("_", " ").title()),
        )
        for s in sectors
    ]

