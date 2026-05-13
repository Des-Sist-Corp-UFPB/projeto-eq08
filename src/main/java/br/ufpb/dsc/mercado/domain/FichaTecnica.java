package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "ficha_tecnica")
public class FichaTecnica {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "O produto pai é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "produto_pai_id", nullable = false)
    private Produto produtoPai;

    @NotNull(message = "O insumo é obrigatório")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "insumo_id", nullable = false)
    private Produto insumo;

    @NotNull(message = "A quantidade é obrigatória")
    @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
    @Column(name = "quantidade", nullable = false, precision = 10, scale = 2)
    private BigDecimal quantidade;

    public FichaTecnica() {}

    public FichaTecnica(Produto produtoPai, Produto insumo, BigDecimal quantidade) {
        this.produtoPai = produtoPai;
        this.insumo = insumo;
        this.quantidade = quantidade;
    }

    // Getters e Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Produto getProdutoPai() { return produtoPai; }
    public void setProdutoPai(Produto produtoPai) { this.produtoPai = produtoPai; }
    public Produto getInsumo() { return insumo; }
    public void setInsumo(Produto insumo) { this.insumo = insumo; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
}
