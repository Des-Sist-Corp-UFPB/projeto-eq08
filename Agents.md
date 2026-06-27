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

## 4. Design System e UI/UX
- Interface premium com estética moderna.
- Presença de *dark mode*.
- Efeitos *glassmorphism*.
- TailwindCSS como framework principal de estilização.

## 5. Páginas Principais (Frontend)
- `Login`: Tela de acesso ao sistema (premium glassmorphic dark mode).
- `RegisterTenant`: Fluxo de onboarding para novos tenants (empresas).
- `DashboardShell`: Área administrativa que inclui o *dashboard overview*, módulo CRUD de colaboradores e visualização de timeline de auditoria.

## 6. Principais Recursos e Módulos (Arquitetura Atual - Módulo 0)
- **Core:** Conexões assíncronas SQLAlchemy suportando SQLite (para testes/dev local) e PostgreSQL (produção).
- **Auditoria:** Rastreamento de ações via tabela `AuditLog`.
- **Usuários:** Autenticação, gestão de tokens de acesso, criação e deleção de usuários com controle de perfil (RBAC).
