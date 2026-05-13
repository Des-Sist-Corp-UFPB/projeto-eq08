package br.ufpb.dsc.mercado.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record FichaTecnicaForm(
    @NotNull(message = "O insumo é obrigatório")
    Long insumoId,

    @NotNull(message = "A quantidade é obrigatória")
    @DecimalMin(value = "0.01", message = "A quantidade deve ser maior que zero")
    BigDecimal quantidade
) {}
