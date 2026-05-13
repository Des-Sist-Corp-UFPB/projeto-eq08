package br.ufpb.dsc.mercado.dto;

import br.ufpb.dsc.mercado.domain.CategoriaProduto;
import br.ufpb.dsc.mercado.domain.UnidadeMedida;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) para criação e edição de produtos.
 */
public record ProdutoForm(

        @NotBlank(message = "O nome é obrigatório")
        @Size(min = 2, max = 120, message = "O nome deve ter entre 2 e 120 caracteres")
        String nome,

        @Size(max = 2000, message = "A descrição pode ter no máximo 2000 caracteres")
        String descricao,

        @DecimalMin(value = "0.00", message = "O preço não pode ser negativo")
        @Digits(integer = 8, fraction = 2, message = "Preço deve ter no máximo 8 dígitos inteiros e 2 decimais")
        BigDecimal precoVenda,

        @NotNull(message = "A unidade de medida é obrigatória")
        UnidadeMedida unidadeMedida,

        @NotNull(message = "A categoria é obrigatória")
        CategoriaProduto categoria,

        boolean perecivel,

        @NotNull(message = "O estoque mínimo é obrigatório")
        @DecimalMin(value = "0.00", message = "O estoque mínimo não pode ser negativo")
        BigDecimal estoqueMinimo

) {
}
