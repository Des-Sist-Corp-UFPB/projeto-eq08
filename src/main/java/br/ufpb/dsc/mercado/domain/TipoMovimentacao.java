package br.ufpb.dsc.mercado.domain;

public enum TipoMovimentacao {
    ENTRADA("Entrada de Estoque"),
    SAIDA("Saída de Estoque");

    private final String descricao;

    TipoMovimentacao(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
