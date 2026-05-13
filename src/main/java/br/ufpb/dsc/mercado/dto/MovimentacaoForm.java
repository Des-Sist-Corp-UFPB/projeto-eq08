package br.ufpb.dsc.mercado.dto;

import br.ufpb.dsc.mercado.domain.TipoMovimentacao;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record MovimentacaoForm(
    @NotNull(message = "O produto é obrigatório")
    Long produtoId,

    @NotNull(message = "O tipo é obrigatório")
    TipoMovimentacao tipo,

    @NotNull(message = "A quantidade é obrigatória")
    @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
    BigDecimal quantidade,

    String motivo,

    LocalDate dataValidade
) {}
