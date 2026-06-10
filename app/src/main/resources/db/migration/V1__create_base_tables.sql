-- ============================================================
-- V1: Tabelas base do sistema (Tenant, Usuários, Segurança)
-- Gestor de Negócio SaaS - EQ08 UFPB
-- ============================================================

-- Habilitar extensão UUID (necessária para gen_random_uuid())
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ========================
-- Tabela: tenants
-- Empresas cadastradas no ecossistema SaaS
-- ========================
CREATE TABLE tenants (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(255) NOT NULL UNIQUE,
    status      VARCHAR(50)  NOT NULL DEFAULT 'active'
                             CHECK (status IN ('active', 'suspended', 'inactive')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_tenants_slug ON tenants (slug);
CREATE INDEX idx_tenants_status ON tenants (status);

-- ========================
-- Tabela: users
-- Contas de acesso de colaboradores e proprietários
-- ========================
CREATE TABLE users (
    id               UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id        UUID        REFERENCES tenants(id) ON DELETE CASCADE,
    name             VARCHAR(255) NOT NULL,
    email            VARCHAR(255) NOT NULL UNIQUE,
    hashed_password  VARCHAR(255) NOT NULL,
    role             VARCHAR(50)  NOT NULL DEFAULT 'OPERATOR'
                                  CHECK (role IN ('SUPER_ADMIN', 'OWNER', 'MANAGER', 'SUPERVISOR', 'OPERATOR')),
    is_active        BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at       TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_users_tenant_id ON users (tenant_id);
CREATE INDEX idx_users_email ON users (email);
CREATE INDEX idx_users_role ON users (role);

-- ========================
-- Tabela: refresh_tokens
-- Sessões ativas para rotação de tokens JWT
-- ========================
CREATE TABLE refresh_tokens (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token       VARCHAR(512) NOT NULL UNIQUE,
    expires_at  TIMESTAMPTZ  NOT NULL,
    is_revoked  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_refresh_tokens_user_id ON refresh_tokens (user_id);
CREATE INDEX idx_refresh_tokens_token ON refresh_tokens (token);

-- ========================
-- Tabela: audit_logs
-- Logs auditáveis de todas as operações críticas
-- ========================
CREATE TABLE audit_logs (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        REFERENCES tenants(id) ON DELETE CASCADE,
    user_id      UUID        REFERENCES users(id) ON DELETE SET NULL,
    action       VARCHAR(255) NOT NULL,
    table_name   VARCHAR(100),
    record_id    VARCHAR(255),
    before_state JSONB,
    after_state  JSONB,
    ip_address   VARCHAR(45),
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_logs_tenant_id ON audit_logs (tenant_id);
CREATE INDEX idx_audit_logs_user_id ON audit_logs (user_id);
CREATE INDEX idx_audit_logs_action ON audit_logs (action);
CREATE INDEX idx_audit_logs_created_at ON audit_logs (created_at DESC);
