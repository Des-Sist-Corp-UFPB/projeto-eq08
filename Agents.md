# Agents.md - Fonte de Verdade do Projeto

Este arquivo contém as informações centrais do projeto e deve ser mantido atualizado continuamente. Ele serve como o contexto e memória principal para agentes de IA operando neste repositório, em conformidade com as regras globais definidas em `GEMINI.md`.

## 1. Visão Geral
Construir uma plataforma modular, moderna, robusta e multitenant para Gestão de PMEs (Pequenos e Médios Negócios), com arquitetura preparada para recursos e automações inteligentes baseadas em IA (*AI-First*). O desenvolvimento é puramente incremental, com meta obrigatória de 100% de cobertura de testes.

## 2. Tecnologias Utilizadas
- **Backend:** Python 3.14+, FastAPI, SQLAlchemy, Alembic, PostgreSQL, Redis, Pytest.
- **Frontend:** React, TypeScript, Vite, TanStack Query, React Router, TailwindCSS.
- **Infraestrutura/Isolamento:** Docker, Docker Compose, Caddy (proxy reverso).

## 3. Regras de Negócio
- **Multitenancy:** Isolamento lógico estrito filtrando todas as tabelas pela coluna chave estrangeira `tenant_id` por meio das dependências de rotas operacionais do FastAPI.
- **Controle de Acesso (RBAC):** Padrão granular estabelecido com os níveis: `SUPER_ADMIN`, `OWNER`, `MANAGER`, `SUPERVISOR`, e `OPERATOR`.
- **Segurança e Sessão:** Autenticação via JWT e Refresh Tokens (armazenados via LocalStorage). Encriptação BCRYPT. O cliente intercepta erros 401 e realiza auto rotação de token.
- **Setor de Negócio (`sector_type`):** Cada tenant declara seu setor no onboarding. Valores aceitos: `food_service` | `retail_apparel` | `generic`. O setor controla: (a) módulos ativos da UI, (b) contexto de BI do copiloto Gemini, (c) KPIs exibidos. Implementado via padrão Strategy em `app/core/sectors/`.

## 4. Design System e UI/UX
- Interface premium com estética moderna.
- Presença de *dark mode*.
- Efeitos *glassmorphism*.
- TailwindCSS como framework principal de estilização.

## 5. Páginas Principais (Frontend)
- `Login`: Tela de acesso ao sistema (premium glassmorphic dark mode).
- `RegisterTenant`: Fluxo de onboarding para novos tenants (empresas).
- `DashboardShell`: Área administrativa que inclui o *dashboard overview*, módulo CRUD de colaboradores e visualização de timeline de auditoria.

## 6. Principais Recursos e Módulos (Arquitetura Atual — Módulo A e B)
- **Core:** Conexões assíncronas SQLAlchemy suportando SQLite (para testes/dev local) e PostgreSQL (produção). Configuração de schemas versionados por Alembic.
- **Auditoria:** Rastreamento de ações via tabela `AuditLog`.
- **Usuários:** Autenticação, gestão de tokens de acesso, criação e deleção de usuários com controle de perfil (RBAC).
- **SectorStrategy (Módulo A):** Padrão Strategy que abstrai o domínio de negócio do tenant. `SectorRegistry` resolve a strategy correta pelo `sector_type` do tenant. Strategies implementadas: `FoodServiceStrategy`, `RetailApparelStrategy`, `GenericStrategy`. Localização: `backend/app/core/sectors/`.
- **BIaaS Engine (Módulo A):** O copiloto Gemini (`app/core/gemini.py`) agora é agnóstico de setor — recebe o contexto construído pela SectorStrategy ativa. O endpoint `/ai/copilot` orquestra: SectorRegistry → build_bi_context() → ask_gemini() → fallback heurístico.
- **Variantes de Produto (Módulo B):** O setor `retail_apparel` utiliza a entidade `ProductVariant` para gerenciar SKU, cor, tamanho, preço específico e estoque. Rotas em `/api/v1/product-variants`. Módulo Insumos desativado na UI para este setor.
