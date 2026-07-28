# ──────────────────────────────────────────────
# Stage 1: builder
# ──────────────────────────────────────────────
FROM python:3.11-slim AS builder

WORKDIR /build

# Instala dependências do sistema necessárias para compilar pacotes C
RUN apt-get update && apt-get install -y --no-install-recommends \
    gcc \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

COPY requirements.txt .
RUN pip install --upgrade pip \
    && pip install --prefix=/install --no-cache-dir -r requirements.txt

# ──────────────────────────────────────────────
# Stage 2: runtime
# ──────────────────────────────────────────────
FROM python:3.11-slim

WORKDIR /app

# Dependências de runtime do PostgreSQL
RUN apt-get update && apt-get install -y --no-install-recommends \
    libpq-dev \
    && rm -rf /var/lib/apt/lists/*

# Copia pacotes instalados do builder
COPY --from=builder /install /usr/local

# Copia código-fonte e scripts
COPY app ./app
COPY seed_db.py .
COPY seed_demo_chat.py .
COPY alembic ./alembic
COPY alembic.ini .
COPY entrypoint.sh .

# Variáveis de ambiente (credenciais injetadas pelo Portainer em runtime)
ENV PYTHONUNBUFFERED=1
ENV PYTHONDONTWRITEBYTECODE=1

EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8080/ping')" || exit 1

CMD ["./entrypoint.sh"]
