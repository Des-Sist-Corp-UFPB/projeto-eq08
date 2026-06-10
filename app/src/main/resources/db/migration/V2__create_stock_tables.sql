-- ============================================================
-- V2: Tabelas de Estoque, Receitas e Vendas (Módulo 1)
-- Gestor de Negócio SaaS - EQ08 UFPB
-- ============================================================

-- ========================
-- Tabela: categories
-- Categorias de insumos e produtos
-- ========================
CREATE TABLE categories (
    id          UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID        NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    name        VARCHAR(255) NOT NULL,
    type        VARCHAR(50)  NOT NULL CHECK (type IN ('INSUMO', 'PRODUCT')),
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categories_tenant_id ON categories (tenant_id);
CREATE INDEX idx_categories_type ON categories (type);

-- ========================
-- Tabela: insumos
-- Inventário físico de insumos/ingredientes
-- ========================
CREATE TABLE insumos (
    id             UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    category_id    UUID         REFERENCES categories(id) ON DELETE SET NULL,
    name           VARCHAR(255) NOT NULL,
    unit           VARCHAR(50)  NOT NULL,
    current_stock  NUMERIC(12,4) NOT NULL DEFAULT 0.0,
    minimum_stock  NUMERIC(12,4) NOT NULL DEFAULT 0.0,
    unit_cost      NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    created_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at     TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_insumos_tenant_id ON insumos (tenant_id);
CREATE INDEX idx_insumos_category_id ON insumos (category_id);

-- ========================
-- Tabela: products
-- Cadastro comercial de produtos de venda
-- ========================
CREATE TABLE products (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID         NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    category_id  UUID         REFERENCES categories(id) ON DELETE SET NULL,
    name         VARCHAR(255) NOT NULL,
    price        NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    is_active    BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMPTZ  NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_products_tenant_id ON products (tenant_id);
CREATE INDEX idx_products_is_active ON products (is_active);

-- ========================
-- Tabela: product_ingredients
-- Ficha Técnica (árvore de composição de receitas)
-- ========================
CREATE TABLE product_ingredients (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    product_id  UUID          NOT NULL REFERENCES products(id) ON DELETE CASCADE,
    insumo_id   UUID          NOT NULL REFERENCES insumos(id) ON DELETE RESTRICT,
    quantity    NUMERIC(12,4) NOT NULL,
    UNIQUE(product_id, insumo_id)
);

CREATE INDEX idx_product_ingredients_product_id ON product_ingredients (product_id);
CREATE INDEX idx_product_ingredients_insumo_id ON product_ingredients (insumo_id);

-- ========================
-- Tabela: orders
-- Histórico de cupons de venda (PDV)
-- ========================
CREATE TABLE orders (
    id           UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID          NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    user_id      UUID          REFERENCES users(id) ON DELETE SET NULL,
    total_price  NUMERIC(10,2) NOT NULL DEFAULT 0.0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_orders_tenant_id ON orders (tenant_id);
CREATE INDEX idx_orders_created_at ON orders (created_at DESC);
CREATE INDEX idx_orders_user_id ON orders (user_id);

-- ========================
-- Tabela: order_items
-- Itens inclusos nas vendas de caixa
-- ========================
CREATE TABLE order_items (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    order_id    UUID          NOT NULL REFERENCES orders(id) ON DELETE CASCADE,
    product_id  UUID          NOT NULL REFERENCES products(id) ON DELETE RESTRICT,
    quantity    INTEGER       NOT NULL CHECK (quantity > 0),
    unit_price  NUMERIC(10,2) NOT NULL
);

CREATE INDEX idx_order_items_order_id ON order_items (order_id);
CREATE INDEX idx_order_items_product_id ON order_items (product_id);

-- ========================
-- Tabela: stock_movements
-- Linha do tempo de todas as movimentações de inventário
-- ========================
CREATE TABLE stock_movements (
    id          UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id   UUID          NOT NULL REFERENCES tenants(id) ON DELETE CASCADE,
    insumo_id   UUID          NOT NULL REFERENCES insumos(id) ON DELETE CASCADE,
    quantity    NUMERIC(12,4) NOT NULL,
    type        VARCHAR(50)   NOT NULL
                              CHECK (type IN ('INPUT', 'OUTPUT', 'ADJUSTMENT', 'AUTOMATIC_CONSUMPTION')),
    reason      VARCHAR(255),
    user_id     UUID          REFERENCES users(id) ON DELETE SET NULL,
    order_id    UUID          REFERENCES orders(id) ON DELETE SET NULL,
    created_at  TIMESTAMPTZ   NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stock_movements_tenant_id ON stock_movements (tenant_id);
CREATE INDEX idx_stock_movements_insumo_id ON stock_movements (insumo_id);
CREATE INDEX idx_stock_movements_type ON stock_movements (type);
CREATE INDEX idx_stock_movements_created_at ON stock_movements (created_at DESC);
