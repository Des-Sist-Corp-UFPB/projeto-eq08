-- ============================================================
-- V4: Tabelas de Escalas, Trocas e Afastamentos (Módulo 3)
-- Gestor de Negócio SaaS - EQ08 UFPB
-- ============================================================

-- ========================
-- Tabela: employee_schedules
-- Escala e turnos de trabalho dos colaboradores
-- ========================
CREATE TABLE employee_schedules (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id     UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    shift_date  DATE        NOT NULL,
    start_time  VARCHAR(10) NOT NULL,
    end_time    VARCHAR(10) NOT NULL,
    notes       VARCHAR(255)
);

CREATE INDEX idx_employee_schedules_tenant_id ON employee_schedules (tenant_id);
CREATE INDEX idx_employee_schedules_user_id ON employee_schedules (user_id);
CREATE INDEX idx_employee_schedules_shift_date ON employee_schedules (shift_date);

-- ========================
-- Tabela: shift_trades
-- Solicitações e transações de troca de turnos
-- ========================
CREATE TABLE shift_trades (
    id                      UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    requesting_user_id      UUID        NOT NULL REFERENCES users(id) ON DELETE RESTRICT,
    target_user_id          UUID        REFERENCES users(id) ON DELETE RESTRICT,
    requesting_schedule_id  UUID        NOT NULL REFERENCES employee_schedules(id) ON DELETE CASCADE,
    target_schedule_id      UUID        REFERENCES employee_schedules(id) ON DELETE CASCADE,
    status                  VARCHAR(50) NOT NULL DEFAULT 'PENDING'
                                        CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    approved_by_id          UUID        REFERENCES users(id) ON DELETE SET NULL,
    created_at              TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at              TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_shift_trades_tenant_id ON shift_trades (tenant_id);
CREATE INDEX idx_shift_trades_requesting_user_id ON shift_trades (requesting_user_id);
CREATE INDEX idx_shift_trades_status ON shift_trades (status);

-- ========================
-- Tabela: absences
-- Férias, faltas e licenças médicas
-- ========================
CREATE TABLE absences (
    id             UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id        UUID        NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    start_date     DATE        NOT NULL,
    end_date       DATE        NOT NULL,
    type           VARCHAR(50) NOT NULL
                               CHECK (type IN ('VACATION', 'MEDICAL_LEAVE', 'ABSENCE', 'OTHER')),
    reason         VARCHAR(255),
    status         VARCHAR(50) NOT NULL DEFAULT 'PENDING'
                               CHECK (status IN ('PENDING', 'APPROVED', 'REJECTED')),
    approved_by_id UUID        REFERENCES users(id) ON DELETE SET NULL,
    CHECK (end_date >= start_date)
);

CREATE INDEX idx_absences_tenant_id ON absences (tenant_id);
CREATE INDEX idx_absences_user_id ON absences (user_id);
CREATE INDEX idx_absences_status ON absences (status);
CREATE INDEX idx_absences_dates ON absences (start_date, end_date);
