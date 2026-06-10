package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Produto de venda com sua Ficha Técnica (ingredientes/receita).
 */
public record Product(
        UUID id,
        UUID tenantId,
        UUID categoryId,
        String categoryName,
        String name,
        BigDecimal price,
        boolean isActive,
        List<ProductIngredient> ingredients,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Product fromResultSet(ResultSet rs) throws SQLException {
        String catIdStr = rs.getString("category_id");
        String catName  = null;
        try { catName = rs.getString("category_name"); } catch (SQLException ignored) {}
        return new Product(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                catIdStr != null ? UUID.fromString(catIdStr) : null,
                catName,
                rs.getString("name"),
                rs.getBigDecimal("price"),
                rs.getBoolean("is_active"),
                new ArrayList<>(),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    /** Calcula o custo de produção baseado nos ingredientes */
    public BigDecimal productionCost() {
        return ingredients.stream()
                .map(ing -> ing.insumoUnitCost().multiply(ing.quantity()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}

// ──────────────────────────────────────────────────────────────────────────────

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
