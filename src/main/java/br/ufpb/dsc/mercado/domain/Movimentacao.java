package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "movimentacao")
public class Movimentacao {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O produto é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lote_id")
    private Lote lote;

    @NotNull(message = "O tipo de movimentação é obrigatório")
    @Enumerated(EnumType.STRING)
    @Column(name = "tipo", nullable = false, length = 20)
    private TipoMovimentacao tipo;

    @NotNull(message = "A quantidade é obrigatória")
    @Column(name = "quantidade", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade;

    @Column(name = "data_movimentacao", nullable = false, updatable = false)
    private Instant dataMovimentacao;

    @Column(name = "motivo", length = 100)
    private String motivo;

    @Column(name = "usuario", length = 50)
    private String usuario;

    @PrePersist
    protected void prePersist() {
        if (this.dataMovimentacao == null) {
            this.dataMovimentacao = Instant.now();
        }
    }

    public Movimentacao() {}

    public Movimentacao(Produto produto, TipoMovimentacao tipo, BigDecimal quantidade, String motivo) {
        this.produto = produto;
        this.tipo = tipo;
        this.quantidade = quantidade;
        this.motivo = motivo;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public Lote getLote() { return lote; }
    public void setLote(Lote lote) { this.lote = lote; }
    public TipoMovimentacao getTipo() { return tipo; }
    public void setTipo(TipoMovimentacao tipo) { this.tipo = tipo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public Instant getDataMovimentacao() { return dataMovimentacao; }
    public void setDataMovimentacao(Instant dataMovimentacao) { this.dataMovimentacao = dataMovimentacao; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
}
