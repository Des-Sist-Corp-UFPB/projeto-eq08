package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Ordem de compra de insumos.
 * status: PENDING | COMPLETED | CANCELLED
 */
public record PurchaseOrder(
        UUID id,
        UUID tenantId,
        UUID supplierId,
        String supplierName,
        String status,
        BigDecimal totalPrice,
        Integer deliveryDays,
        Integer qualityRating,
        Integer priceRating,
        List<PurchaseItem> items,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static PurchaseOrder fromResultSet(ResultSet rs) throws SQLException {
        String supName = null;
        try { supName = rs.getString("supplier_name"); } catch (SQLException ignored) {}
        return new PurchaseOrder(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("supplier_id", UUID.class),
                supName,
                rs.getString("status"),
                rs.getBigDecimal("total_price"),
                (Integer) rs.getObject("delivery_days"),
                (Integer) rs.getObject("quality_rating"),
                (Integer) rs.getObject("price_rating"),
                new ArrayList<>(),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}

