import logging
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime, timezone
from app.api.api import api_router
from app.core.config import settings

# ── Configuração Profissional de Observabilidade (Logs) ────────────────────
class IgnoreProxyUpgradeFilter(logging.Filter):
    """
    Filtro nativo para suprimir warnings de 'Unsupported upgrade request' 
    gerados pelo Uvicorn quando proxies reversos (como Caddy/Nginx) tentam 
    fazer probing com Upgrade: h2c (HTTP/2) não suportado pelo Uvicorn.
    Garante que o I/O do log não engargale a aplicação.
    """
    def filter(self, record: logging.LogRecord) -> bool:
        return "Unsupported upgrade request" not in record.getMessage()

# Aplica o filtro no logger interno de erros do Uvicorn
logging.getLogger("uvicorn.error").addFilter(IgnoreProxyUpgradeFilter())
# ─────────────────────────────────────────────────────────────────────────────

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    description="SaaS de Gestão modular e multitenant para PMEs com arquitetura AI-First.",
)

# Setup CORS middleware
if settings.BACKEND_CORS_ORIGINS:
    app.add_middleware(
        CORSMiddleware,
        allow_origins=[str(origin).rstrip("/") for origin in settings.BACKEND_CORS_ORIGINS],
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )


@app.get("/ping", tags=["health"])
async def ping():
    """
    Public health check endpoint — required for server status monitoring.
    No authentication required.
    """
    return {
        "status": "ok",
        "service": "eq08",
        "timestamp": datetime.now(timezone.utc).isoformat().replace("+00:00", "Z"),
    }


@app.get("/health", tags=["health"])
async def health_check():
    """
    Service health check endpoint.
    Verifica conectividade real com o banco de dados (SELECT 1).
    Retorna 503 se o banco estiver inacessível.
    """
    from sqlalchemy import text
    from app.core.database import SessionLocal

    db_status = "healthy"
    http_status = 200

    try:
        async with SessionLocal() as session:
            await session.execute(text("SELECT 1"))
    except Exception as exc:
        db_status = "unreachable"
        http_status = 503
        logging.getLogger("uvicorn.error").warning(
            "Healthcheck DB falhou: %s", str(exc)
        )

    from fastapi.responses import JSONResponse
    return JSONResponse(
        status_code=http_status,
        content={
            "status": "healthy" if http_status == 200 else "unhealthy",
            "project": settings.PROJECT_NAME,
            "version": "1.0.0",
            "checks": {
                "database": db_status,
            },
        },
    )


from fastapi.staticfiles import StaticFiles
from fastapi.responses import FileResponse
from fastapi import HTTPException
import os

# Include all routers
app.include_router(api_router, prefix=settings.API_V1_STR)

from fastapi import WebSocket

@app.websocket("/{path_name:path}")
async def websocket_catch_all(websocket: WebSocket, path_name: str):
    """
    Absorve tentativas de conexão WebSocket (como o HMR do Vite cliente ou extensões),
    aceitando e fechando imediatamente para evitar flood de 'Unsupported upgrade request' no uvicorn.
    """
    await websocket.accept()
    await websocket.close()

# Serve Frontend SPA
static_dir = os.path.join(os.path.dirname(__file__), "..", "static")
if os.path.isdir(static_dir):
    app.mount("/assets", StaticFiles(directory=os.path.join(static_dir, "assets")), name="assets")
    
    @app.api_route("/{path_name:path}", methods=["GET"])
    async def catch_all(path_name: str):
        if path_name.startswith("api/") or path_name in ["ping", "health", "docs", "openapi.json"]:
            raise HTTPException(status_code=404, detail="Not Found")
        
        file_path = os.path.join(static_dir, path_name)
        if os.path.isfile(file_path):
            return FileResponse(file_path)
            
        return FileResponse(os.path.join(static_dir, "index.html"))
