package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Item da Ficha Técnica: ingrediente de um produto.
 */
public record ProductIngredient(
        UUID id,
        UUID productId,
        UUID insumoId,
        String insumoName,
        String insumoUnit,
        BigDecimal quantity,
        BigDecimal insumoUnitCost
) {
    public static ProductIngredient fromResultSet(ResultSet rs) throws SQLException {
        return new ProductIngredient(
                rs.getObject("id", UUID.class),
                rs.getObject("product_id", UUID.class),
                rs.getObject("insumo_id", UUID.class),
                rs.getString("insumo_name"),
                rs.getString("insumo_unit"),
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("unit_cost")
        );
    }
}
