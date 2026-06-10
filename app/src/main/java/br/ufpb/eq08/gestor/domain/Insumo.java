package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Insumo: ingrediente/material de estoque.
 */
public record Insumo(
        UUID id,
        UUID tenantId,
        UUID categoryId,
        String categoryName,
        String name,
        String unit,
        BigDecimal currentStock,
        BigDecimal minimumStock,
        BigDecimal unitCost,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Insumo fromResultSet(ResultSet rs) throws SQLException {
        String catIdStr = rs.getString("category_id");
        String catName  = null;
        try { catName = rs.getString("category_name"); } catch (SQLException ignored) {}
        return new Insumo(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                catIdStr != null ? UUID.fromString(catIdStr) : null,
                catName,
                rs.getString("name"),
                rs.getString("unit"),
                rs.getBigDecimal("current_stock"),
                rs.getBigDecimal("minimum_stock"),
                rs.getBigDecimal("unit_cost"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    public boolean isBelowMinimum() {
        return currentStock.compareTo(minimumStock) < 0;
    }
}

