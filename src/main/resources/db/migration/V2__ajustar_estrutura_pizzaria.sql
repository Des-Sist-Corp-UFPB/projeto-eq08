-- Migração V2: Ajuste da estrutura para sistema de pizzaria
-- Adiciona suporte a categorias, unidades de medida, lotes, movimentações e ficha técnica.

-- 1. Ajustes na tabela produto
ALTER TABLE produto ADD COLUMN unidade_medida VARCHAR(20) NOT NULL DEFAULT 'UN';
ALTER TABLE produto ADD COLUMN categoria VARCHAR(30) NOT NULL DEFAULT 'INSUMO';
ALTER TABLE produto ADD COLUMN perecivel BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE produto ADD COLUMN estoque_minimo NUMERIC(10, 2) NOT NULL DEFAULT 0;
ALTER TABLE produto RENAME COLUMN preco TO preco_venda;
ALTER TABLE produto ALTER COLUMN preco_venda DROP NOT NULL; -- Insumos podem não ter preço de venda

-- 2. Tabela de Lotes (Estoque real por validade)
CREATE TABLE lote (
    id                BIGSERIAL PRIMARY KEY,
    produto_id        BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE,
    quantidade_atual  NUMERIC(10, 2) NOT NULL CHECK (quantidade_atual >= 0),
    data_validade     DATE,
    data_entrada      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    criado_em         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    atualizado_em     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_lote_produto_id ON lote(produto_id);
CREATE INDEX idx_lote_validade ON lote(data_validade);

-- 3. Tabela de Movimentações (Turnover)
CREATE TABLE movimentacao (
    id                BIGSERIAL PRIMARY KEY,
    produto_id        BIGINT NOT NULL REFERENCES produto(id),
    lote_id           BIGINT REFERENCES lote(id),
    tipo              VARCHAR(20) NOT NULL, -- ENTRADA, SAIDA
    quantidade        NUMERIC(10, 2) NOT NULL,
    data_movimentacao TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW(),
    motivo            VARCHAR(100), -- COMPRA, VENDA, DESCARTE, AJUSTE
    usuario           VARCHAR(50) -- Opcional: quem realizou
);

CREATE INDEX idx_movimentacao_produto_id ON movimentacao(produto_id);
CREATE INDEX idx_movimentacao_data ON movimentacao(data_movimentacao);

-- 4. Tabela de Ficha Técnica (Receitas)
CREATE TABLE ficha_tecnica (
    id                BIGSERIAL PRIMARY KEY,
    produto_pai_id    BIGINT NOT NULL REFERENCES produto(id) ON DELETE CASCADE, -- Ex: Pizza Margherita
    insumo_id         BIGINT NOT NULL REFERENCES produto(id), -- Ex: Queijo Mussarela
    quantidade        NUMERIC(10, 2) NOT NULL CHECK (quantidade > 0),
    UNIQUE(produto_pai_id, insumo_id)
);

CREATE INDEX idx_ficha_tecnica_pai ON ficha_tecnica(produto_pai_id);

COMMENT ON TABLE lote IS 'Armazena as quantidades de produtos segregadas por validade';
COMMENT ON TABLE movimentacao IS 'Histórico de todas as entradas e saídas do estoque';
COMMENT ON TABLE ficha_tecnica IS 'Define quais insumos e em qual quantidade compõem um produto final';
