-- ============================================================
-- V5: Tabelas de IA e Previsões (Módulo 4)
-- Gestor de Negócio SaaS - EQ08 UFPB
-- ============================================================

-- ========================
-- Tabela: demand_forecasts
-- Série temporal preditiva de vendas
-- ========================
CREATE TABLE demand_forecasts (
    id                UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id         UUID          NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    target_date       DATE          NOT NULL,
    predicted_orders  INTEGER       NOT NULL DEFAULT 0,
    predicted_revenue NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    confidence_score  NUMERIC(3,2)  NOT NULL DEFAULT 0.0
                                    CHECK (confidence_score BETWEEN 0.0 AND 1.0),
    model_version     VARCHAR(50)   NOT NULL DEFAULT '1.0',
    created_at        TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    UNIQUE(tenant_id, target_date)
);

CREATE INDEX idx_demand_forecasts_tenant_id ON demand_forecasts (tenant_id);
CREATE INDEX idx_demand_forecasts_target_date ON demand_forecasts (target_date);

-- ========================
-- Tabela: ai_recommendations
-- Insights e ações automatizadas geradas pelo scorecard do sistema
-- ========================
CREATE TABLE ai_recommendations (
    id            UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    type          VARCHAR(50) NOT NULL
                              CHECK (type IN ('STOCK_REPLENISHMENT', 'SHIFT_OPTIMIZATION')),
    title         VARCHAR(255) NOT NULL,
    description   TEXT         NOT NULL,
    impact_level  VARCHAR(50) NOT NULL
                              CHECK (impact_level IN ('HIGH', 'MEDIUM', 'LOW')),
    action_data   JSONB,
    status        VARCHAR(50) NOT NULL DEFAULT 'PENDING'
                              CHECK (status IN ('PENDING', 'APPLIED', 'DISMISSED')),
    created_at    TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ai_recommendations_tenant_id ON ai_recommendations (tenant_id);
CREATE INDEX idx_ai_recommendations_type ON ai_recommendations (type);
CREATE INDEX idx_ai_recommendations_status ON ai_recommendations (status);
CREATE INDEX idx_ai_recommendations_impact_level ON ai_recommendations (impact_level);
