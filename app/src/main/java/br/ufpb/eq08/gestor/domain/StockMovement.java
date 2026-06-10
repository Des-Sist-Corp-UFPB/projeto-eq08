package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Movimentação de estoque de um insumo.
 * type: INPUT | OUTPUT | ADJUSTMENT | AUTOMATIC_CONSUMPTION
 */
public record StockMovement(
        UUID id,
        UUID tenantId,
        UUID insumoId,
        String insumoName,
        BigDecimal quantity,
        String type,
        String reason,
        UUID userId,
        String userName,
        UUID orderId,
        OffsetDateTime createdAt
) {
    public static StockMovement fromResultSet(ResultSet rs) throws SQLException {
        String insumoName = null;
        try { insumoName = rs.getString("insumo_name"); } catch (SQLException ignored) {}
        String userName = null;
        try { userName = rs.getString("user_name"); } catch (SQLException ignored) {}
        String userIdStr = rs.getString("user_id");
        String orderIdStr = rs.getString("order_id");
        return new StockMovement(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("insumo_id", UUID.class),
                insumoName,
                rs.getBigDecimal("quantity"),
                rs.getString("type"),
                rs.getString("reason"),
                userIdStr != null ? UUID.fromString(userIdStr) : null,
                userName,
                orderIdStr != null ? UUID.fromString(orderIdStr) : null,
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}
