package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

@Entity
@Table(name = "lote")
public class Lote {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O produto é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_id", nullable = false)
    private Produto produto;

    @NotNull(message = "A quantidade é obrigatória")
    @DecimalMin(value = "0.00", message = "A quantidade não pode ser negativa")
    @Column(name = "quantidade_atual", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidadeAtual;

    @Column(name = "data_validade")
    private LocalDate dataValidade;

    @Column(name = "data_entrada", nullable = false, updatable = false)
    private Instant dataEntrada;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @PrePersist
    protected void prePersist() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
        if (this.dataEntrada == null) {
            this.dataEntrada = agora;
        }
    }

    @PreUpdate
    protected void preUpdate() {
        this.atualizadoEm = Instant.now();
    }

    public Lote() {}

    public Lote(Produto produto, BigDecimal quantidadeAtual, LocalDate dataValidade) {
        this.produto = produto;
        this.quantidadeAtual = quantidadeAtual;
        this.dataValidade = dataValidade;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Produto getProduto() { return produto; }
    public void setProduto(Produto produto) { this.produto = produto; }
    public BigDecimal getQuantidadeAtual() { return quantidadeAtual; }
    public void setQuantidadeAtual(BigDecimal quantidadeAtual) { this.quantidadeAtual = quantidadeAtual; }
    public LocalDate getDataValidade() { return dataValidade; }
    public void setDataValidade(LocalDate dataValidade) { this.dataValidade = dataValidade; }
    public Instant getDataEntrada() { return dataEntrada; }
    public void setDataEntrada(Instant dataEntrada) { this.dataEntrada = dataEntrada; }
}
