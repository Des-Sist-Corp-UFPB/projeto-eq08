package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Item de um cupom de venda.
 */
public record OrderItem(
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
