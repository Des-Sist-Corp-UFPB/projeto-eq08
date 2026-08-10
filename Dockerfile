# ──────────────────────────────────────────────
# Stage 1: Build Frontend (Vite)
# ──────────────────────────────────────────────
FROM node:20-slim AS frontend-builder
WORKDIR /app

# Instala dependências do frontend
COPY frontend/package*.json ./
RUN npm ci

# Compila o frontend
COPY frontend/ ./
ENV VITE_API_URL=/api/v1
RUN npm run build

# ──────────────────────────────────────────────
# Stage 2: Build Backend Dependencies
# ──────────────────────────────────────────────
FROM python:3.11-slim AS backend-builder
WORKDIR /build

# Instala dependências do sistema necessárias para pacotes C
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY backend/requirements.txt .
RUN pip install --upgrade pip \
    && pip install --prefix=/install --no-cache-dir -r requirements.txt aiofiles \
    && PYTHONPATH=/install/lib/python3.11/site-packages pip install --prefix=/install --no-cache-dir opentelemetry-distro opentelemetry-exporter-otlp \
    && PYTHONPATH=/install/lib/python3.11/site-packages /install/bin/opentelemetry-bootstrap -a requirements | pip install --prefix=/install --no-cache-dir -r /dev/stdin

# ──────────────────────────────────────────────
# Stage 3: Runtime Unificado (Backend + Frontend)
# ──────────────────────────────────────────────
FROM python:3.11-slim
WORKDIR /app

# Dependências de runtime do PostgreSQL
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Copia pacotes instalados do backend-builder
COPY --from=backend-builder /install /usr/local

# Copia código-fonte do backend
COPY backend/app ./app
COPY backend/seed_db.py .
COPY backend/seed_demo_chat.py .
COPY backend/alembic ./alembic
COPY backend/alembic.ini .
COPY backend/entrypoint.sh .

# Copia o build do frontend para a pasta static do backend
COPY --from=frontend-builder /app/dist ./static

# Variáveis de ambiente
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8080/health')" || exit 1

CMD ["./entrypoint.sh"]
