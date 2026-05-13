package br.ufpb.dsc.mercado.domain;

public enum UnidadeMedida {
    KG("Quilograma"),
    G("Grama"),
    L("Litro"),
    ML("Mililitro"),
    UN("Unidade"),
    PCT("Pacote"),
    CX("Caixa");

    private final String descricao;

    UnidadeMedida(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
