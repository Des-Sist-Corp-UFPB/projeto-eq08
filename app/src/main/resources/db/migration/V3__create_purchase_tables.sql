-- ============================================================
-- V3: Tabelas de Fornecedores e Compras (Módulo 2)
-- Gestor de Negócio SaaS - EQ08 UFPB
-- ============================================================

-- ========================
-- Tabela: suppliers
-- Cadastro de fornecedores parceiros comerciais
-- ========================
CREATE TABLE suppliers (
    id            UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id     UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name          VARCHAR(255) NOT NULL,
    document      VARCHAR(50),
    phone         VARCHAR(50),
    email         VARCHAR(255),
    contact_name  VARCHAR(255),
    created_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at    TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_suppliers_tenant_id ON suppliers (tenant_id);

-- ========================
-- Tabela: purchase_orders
-- Registro de notas e ordens de compra de insumos
-- ========================
CREATE TABLE purchase_orders (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    supplier_id     UUID          NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    status          VARCHAR(50)   NOT NULL DEFAULT 'PENDING'
                                  CHECK (status IN ('PENDING', 'COMPLETED', 'CANCELLED')),
    total_price     NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    delivery_days   INTEGER,
    quality_rating  INTEGER       CHECK (quality_rating BETWEEN 1 AND 5),
    price_rating    INTEGER       CHECK (price_rating BETWEEN 1 AND 5),
    created_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_purchase_orders_tenant_id ON purchase_orders (tenant_id);
CREATE INDEX idx_purchase_orders_supplier_id ON purchase_orders (supplier_id);
CREATE INDEX idx_purchase_orders_status ON purchase_orders (status);
CREATE INDEX idx_purchase_orders_created_at ON purchase_orders (created_at DESC);

-- ========================
-- Tabela: purchase_items
-- Cesta de insumos contidos no pedido de compra
-- ========================
CREATE TABLE purchase_items (
    id                 UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    purchase_order_id  UUID          NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    insumo_id          UUID          NOT NULL REFERENCES insumos(id) ON DELETE RESTRICT,
    quantity           NUMERIC(12,4) NOT NULL CHECK (quantity > 0),
    unit_cost          NUMERIC(10,2) NOT NULL CHECK (unit_cost >= 0)
);

CREATE INDEX idx_purchase_items_purchase_order_id ON purchase_items (purchase_order_id);
CREATE INDEX idx_purchase_items_insumo_id ON purchase_items (insumo_id);
