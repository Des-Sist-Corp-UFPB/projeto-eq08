# gestor_negocio

Este repositório tem como objetivo armazenar o código base de um software para auxiliar na gestão de pequenos negócios.

Plataforma SaaS modular, moderna e multitenant para Gestão de PMEs (Pequenos e Médios Negócios), com arquitetura preparada para recursos e automações inteligentes baseadas em IA (*AI-First*).

---

## Stack Tecnológica

- **Backend:** Python 3.14+, FastAPI, SQLAlchemy (async), Alembic, PostgreSQL, Pytest
- **Frontend:** React, TypeScript, Vite, TanStack Query, React Router, TailwindCSS
- **Infraestrutura:** Docker, Docker Compose, Caddy (proxy reverso), GitHub Actions (CI/CD)

---

## Log de Auditoria

### O que é auditado

O sistema registra automaticamente as seguintes ações de usuário:

| Ação | Descrição |
|---|---|
| `USER_LOGIN` | Login bem-sucedido no sistema |
| `REFRESH_TOKEN` | Renovação de token de acesso |
| `USER_CREATE` | Criação de novo colaborador |
| `USER_UPDATE` | Atualização de dados de usuário |
| `USER_DELETE` | Exclusão de usuário |
| `CATEGORY_CREATE` | Criação de categoria de produto |
| `CATEGORY_UPDATE` | Atualização de categoria |
| `CATEGORY_DELETE` | Exclusão de categoria |
| `PRODUCT_CREATE` | Cadastro de produto |
| `PRODUCT_UPDATE` | Atualização de produto |
| `PRODUCT_DELETE` | Exclusão de produto |
| `INSUMO_CREATE` | Cadastro de insumo/matéria-prima |
| `INSUMO_UPDATE` | Atualização de insumo |
| `INSUMO_DELETE` | Exclusão de insumo |
| `STOCK_MOVEMENT` | Movimentação manual de estoque |
| `SUPPLIER_CREATE` | Cadastro de fornecedor |
| `SUPPLIER_UPDATE` | Atualização de fornecedor |
| `SUPPLIER_DELETE` | Exclusão de fornecedor |
| `PURCHASE_ORDER_CREATE` | Criação de ordem de compra |
| `PURCHASE_ORDER_STATUS_UPDATE` | Atualização de status de ordem de compra |
| `ORDER_CREATE` | Registro de pedido/venda |
| `SCHEDULE_CREATE` | Criação de escala de turno |
| `SCHEDULE_UPDATE` | Atualização de escala |
| `SCHEDULE_DELETE` | Exclusão de escala |
| `AI_RECOMMENDATION_APPLY` | Aplicação de recomendação de IA |
| `AI_RECOMMENDATION_DISMISS` | Descarte de recomendação de IA |

### Onde fica armazenado

Os registros são persistidos na tabela **`audit_logs`** do banco de dados PostgreSQL (ou SQLite no ambiente de desenvolvimento/testes). Principais campos:

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID | Identificador único do registro |
| `tenant_id` | UUID (FK) | Empresa à qual o evento pertence (isolamento multitenant) |
| `user_id` | UUID (FK, nullable) | Usuário que executou a ação |
| `action` | String(255) | Código da ação auditada (ex: `USER_LOGIN`) |
| `table_name` | String(100) | Tabela afetada pela ação (ex: `users`) |
| `record_id` | String(255) | ID do registro afetado |
| `before_state` | JSON | Estado do objeto antes da modificação |
| `after_state` | JSON | Estado do objeto após a modificação |
| `ip_address` | String(45) | Endereço IP do solicitante |
| `created_at` | DateTime (UTC) | Timestamp do evento |

### Como foi implementado

A auditoria foi implementada via **serviço dedicado** (`create_audit_log`): uma função assíncrona reutilizável que é chamada explicitamente nos endpoints da API imediatamente após cada operação de escrita bem-sucedida. Não há middleware global — cada endpoint é responsável por registrar sua própria ação, garantindo rastreabilidade granular e controle fino sobre o que é auditado.

A leitura dos logs é protegida por RBAC e restrita aos papéis `OWNER`, `MANAGER` e `SUPERVISOR`.

### Arquivos participantes

| Arquivo | Papel |
|---|---|
| `backend/app/models/audit.py` | Model SQLAlchemy — define a tabela `audit_logs` |
| `backend/app/crud/crud_audit.py` | Serviço de auditoria — `create_audit_log()` e `get_audit_logs_by_tenant()` |
| `backend/app/api/endpoints/audit.py` | Endpoint REST `GET /api/v1/audit/` — listagem paginada dos logs |
| `backend/app/api/endpoints/auth.py` | Registra eventos de login e refresh de token |
| `backend/app/api/endpoints/users.py` | Registra criação, atualização e exclusão de usuários |
| `backend/app/api/endpoints/categories.py` | Registra operações sobre categorias |
| `backend/app/api/endpoints/products.py` | Registra operações sobre produtos |
| `backend/app/api/endpoints/insumos.py` | Registra operações sobre insumos e movimentações de estoque |
| `backend/app/api/endpoints/suppliers.py` | Registra operações sobre fornecedores |
| `backend/app/api/endpoints/purchases.py` | Registra criação e atualização de ordens de compra |
| `backend/app/api/endpoints/orders.py` | Registra criação de pedidos/vendas |
| `backend/app/api/endpoints/schedules.py` | Registra operações sobre escalas de turnos |
| `backend/app/api/endpoints/ai.py` | Registra aplicação e descarte de recomendações de IA |
| `backend/app/api/deps.py` | Define `RoleChecker` — controla o acesso RBAC ao endpoint de listagem |

---

## Integração com Serviço Externo

### Serviço 1: Google Gemini API (IA Generativa)

**Qual é o serviço externo:**
O sistema integra-se com a **Google Gemini API** (`google-genai`) — serviço de IA generativa da Google — para alimentar o módulo de **Copiloto Inteligente de Negócios**.

**Para que é usado:**
O Copiloto recebe uma pergunta do gestor, monta um contexto em tempo real com dados do negócio (faturamento, estoque crítico, fornecedores, escalas do dia) e consulta o modelo `gemini-2.0-flash` para gerar respostas factuais e contextualizadas. Quando a chave não está configurada, o sistema opera em modo heurístico de fallback sem dependência externa.

**Arquivos participantes:**

| Arquivo | Papel |
|---------|-------|
| `backend/app/core/gemini.py` | Serviço de integração — `build_business_context()` e `ask_gemini()` |
| `backend/app/api/endpoints/ai.py` | Endpoint `POST /api/v1/ai/copilot` — orquestra consulta ao Gemini |

**Como é configurado:**

| Variável de Ambiente | Descrição |
|----------------------|-----------|
| `GEMINI_API_KEY` | Chave da API Google Gemini (opcional — sem ela, o sistema usa respostas heurísticas) |

---

## Cobertura de Testes

**Cobertura total: 91%** (74 testes, backend Python/FastAPI)

O relatório completo está commitado em [`cobertura/backend/index.html`](cobertura/backend/index.html).

| Módulo | Cobertura |
|--------|-----------|
| `app/models/` | 100% |
| `app/schemas/` | 100% |
| `app/crud/` | ~92% |
| `app/api/endpoints/` | ~89% |
| `app/core/` | ~94% |
| **TOTAL** | **91%** |

Para reproduzir localmente:
```bash
cd backend
JWT_SECRET=local DB_USER=local DB_PASSWORD=local DATABASE_URL=sqlite+aiosqlite:///:memory: \
  PYTHONPATH=. pytest tests/ --cov=app --cov-report=html --cov-report=term-missing
cp -r htmlcov/ ../cobertura/backend/
```
