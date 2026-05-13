package br.ufpb.dsc.mercado.domain;

public enum CategoriaProduto {
    INSUMO("Insumo/Ingrediente"),
    PRODUTO_FINAL("Produto para Venda"),
    BEBIDA("Bebida"),
    OUTROS("Outros");

    private final String descricao;

    CategoriaProduto(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
