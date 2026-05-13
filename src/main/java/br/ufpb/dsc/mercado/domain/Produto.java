package br.ufpb.dsc.mercado.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Entidade JPA que representa um produto no sistema.
 *
 * <p><strong>O que é uma Entidade JPA?</strong><br>
 * Uma entidade é uma classe Java mapeada para uma tabela do banco de dados.
 * Cada instância da classe corresponde a uma linha da tabela.
 * O JPA (Java Persistence API) é a especificação; o Hibernate é a implementação usada pelo Spring Boot.
 *
 * <p><strong>Ciclo de vida de uma entidade JPA:</strong>
 * <ol>
 *   <li><em>Transient</em>: objeto criado com {@code new}, ainda não gerenciado pelo JPA.</li>
 *   <li><em>Managed</em>: objeto salvo/buscado pelo EntityManager — mudanças são sincronizadas com o banco.</li>
 *   <li><em>Detached</em>: objeto foi gerenciado mas a sessão foi fechada.</li>
 *   <li><em>Removed</em>: marcado para exclusão.</li>
 * </ol>
 *
 * @author DSC - UFPB Campus IV
 */
// @Entity informa ao JPA que esta classe é uma entidade persistível.
@Entity
// @Table define o nome exato da tabela no banco. Sem ela, o JPA usaria o nome da classe.
@Table(name = "produto")
public class Produto {

    /**
     * Identificador único do produto.
     *
     * <p>{@code @Id} marca o campo como chave primária.
     * {@code @GeneratedValue} delega a geração do ID ao banco via SEQUENCE do PostgreSQL
     * (equivalente ao BIGSERIAL da migração SQL).
     * {@code GenerationType.IDENTITY} usa a coluna de identidade/autoincremento do banco.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nome do produto.
     *
     * <p>{@code @Column} permite configurar restrições a nível de DDL (geração de schema).
     * Como usamos Flyway para controlar o schema, esses atributos servem mais como documentação
     * e para validação do modelo.
     *
     * <p>{@code @NotBlank} (Bean Validation) garante que o nome não seja nulo, vazio ou só espaços.
     * {@code @Size} limita o tamanho mínimo e máximo da string.
     */
    @NotBlank(message = "O nome do produto é obrigatório")
    @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
    @Column(name = "nome", nullable = false, length = 120)
    private String nome;

    /**
     * Descrição opcional do produto.
     *
     * <p>{@code columnDefinition = "TEXT"} mapeia para o tipo TEXT do PostgreSQL,
     * que suporta strings de tamanho ilimitado (diferente de VARCHAR).
     */
    @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres")
    @Column(name = "descricao", columnDefinition = "TEXT")
    private String descricao;

    @NotNull(message = "A unidade de medida é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "unidade_medida", nullable = false, length = 20)
    private UnidadeMedida unidadeMedida = UnidadeMedida.UN;

    @NotNull(message = "A categoria é obrigatória")
    @Enumerated(EnumType.STRING)
    @Column(name = "categoria", nullable = false, length = 30)
    private CategoriaProduto categoria = CategoriaProduto.INSUMO;

    @Column(name = "perecivel", nullable = false)
    private boolean perecivel = false;

    @NotNull(message = "O estoque mínimo é obrigatório")
    @DecimalMin(value = "0.00", message = "O estoque mínimo não pode ser negativo")
    @Column(name = "estoque_minimo", nullable = false, precision = 10, scale = 2)
    private BigDecimal estoqueMinimo = BigDecimal.ZERO;

    /**
     * Preço de venda do produto.
     * Opcional para insumos.
     */
    @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
    @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
    @Column(name = "preco_venda", precision = 10, scale = 2)
    private BigDecimal precoVenda;

    /**
     * Data e hora de criação do registro.
     *
     * <p>{@code Instant} representa um ponto no tempo em UTC — a melhor prática para armazenar
     * timestamps no banco, independente do fuso horário do servidor.
     *
     * <p>{@code updatable = false} impede que o Hibernate atualize este campo após a criação.
     */
    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    /**
     * Data e hora da última atualização do registro.
     */
    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    // =========================================================================
    // CALLBACKS JPA (@PrePersist / @PreUpdate)
    // =========================================================================

    /**
     * Executado pelo JPA automaticamente ANTES de fazer INSERT no banco.
     *
     * <p>Garante que as datas de criação e atualização sejam preenchidas
     * sem que o código de negócio precise se preocupar com isso.
     */
    @PrePersist
    protected void prePersist() {
        Instant agora = Instant.now();
        this.criadoEm = agora;
        this.atualizadoEm = agora;
    }

    /**
     * Executado pelo JPA automaticamente ANTES de fazer UPDATE no banco.
     *
     * <p>Atualiza automaticamente o campo {@code atualizadoEm} a cada modificação.
     */
    @PreUpdate
    protected void preUpdate() {
        this.atualizadoEm = Instant.now();
    }

    // =========================================================================
    // CONSTRUTORES
    // =========================================================================

    /**
     * Construtor padrão exigido pelo JPA.
     * O JPA precisa instanciar a entidade via reflexão ao carregar do banco.
     */
    public Produto() {
    }

    public Produto(String nome, String descricao, BigDecimal precoVenda, UnidadeMedida unidadeMedida, CategoriaProduto categoria) {
        this.nome = nome;
        this.descricao = descricao;
        this.precoVenda = precoVenda;
        this.unidadeMedida = unidadeMedida;
        this.categoria = categoria;
    }

    // =========================================================================
    // GETTERS E SETTERS
    // =========================================================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public BigDecimal getPrecoVenda() {
        return precoVenda;
    }

    public void setPrecoVenda(BigDecimal precoVenda) {
        this.precoVenda = precoVenda;
    }

    public UnidadeMedida getUnidadeMedida() {
        return unidadeMedida;
    }

    public void setUnidadeMedida(UnidadeMedida unidadeMedida) {
        this.unidadeMedida = unidadeMedida;
    }

    public CategoriaProduto getCategoria() {
        return categoria;
    }

    public void setCategoria(CategoriaProduto categoria) {
        this.categoria = categoria;
    }

    public boolean isPerecivel() {
        return perecivel;
    }

    public void setPerecivel(boolean perecivel) {
        this.perecivel = perecivel;
    }

    public BigDecimal getEstoqueMinimo() {
        return estoqueMinimo;
    }

    public void setEstoqueMinimo(BigDecimal estoqueMinimo) {
        this.estoqueMinimo = estoqueMinimo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }

    @Override
    public String toString() {
        return "Produto{id=" + id + ", nome='" + nome + "', preco=" + preco + "}";
    }
}
