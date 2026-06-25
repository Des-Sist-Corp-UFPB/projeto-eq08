from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from datetime import datetime, timezone
from app.api.api import api_router
from app.core.config import settings

app = FastAPI(
    title=settings.PROJECT_NAME,
    openapi_url=f"{settings.API_V1_STR}/openapi.json",
    description="SaaS de Gestão modular e multitenant para PMEs com arquitetura AI-First."
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
    """
    return {
        "status": "healthy",
        "project": settings.PROJECT_NAME,
        "version": "1.0.0"
    }


# Include all routers
app.include_router(api_router, prefix=settings.API_V1_STR)
