#!/bin/bash
set -e

echo "⏳ Executando migrações do banco de dados (Alembic)..."
alembic upgrade head

echo "🚀 Iniciando servidor Uvicorn..."
exec uvicorn app.main:app --host 0.0.0.0 --port 8080
