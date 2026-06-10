package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Cupom de venda (PDV).
 */
public record Order(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userName,
        BigDecimal totalPrice,
        List<OrderItem> items,
        OffsetDateTime createdAt
) {
    public static Order fromResultSet(ResultSet rs) throws SQLException {
        String userIdStr = rs.getString("user_id");
        String userName  = null;
        try { userName = rs.getString("user_name"); } catch (SQLException ignored) {}
        return new Order(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                userIdStr != null ? UUID.fromString(userIdStr) : null,
                userName,
                rs.getBigDecimal("total_price"),
                new ArrayList<>(),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}

// ──────────────────────────────────────────────────────────────────────────────

/**
 * Item de um cupom de venda.
 */
record OrderItem(
        UUID id,
        UUID orderId,
        UUID productId,
        String productName,
        int quantity,
        BigDecimal unitPrice
) {
    public static OrderItem fromResultSet(ResultSet rs) throws SQLException {
        String pName = null;
        try { pName = rs.getString("product_name"); } catch (SQLException ignored) {}
        return new OrderItem(
                rs.getObject("id", UUID.class),
                rs.getObject("order_id", UUID.class),
                rs.getObject("product_id", UUID.class),
                pName,
                rs.getInt("quantity"),
                rs.getBigDecimal("unit_price")
        );
    }

    public BigDecimal subtotal() {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }
}
