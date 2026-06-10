package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

/**
 * Item (insumo) contido em uma ordem de compra.
 */
public record PurchaseItem(
        UUID id,
        UUID purchaseOrderId,
        UUID insumoId,
        String insumoName,
        String insumoUnit,
        BigDecimal quantity,
        BigDecimal unitCost
) {
    public static PurchaseItem fromResultSet(ResultSet rs) throws SQLException {
        String iName = null;
        try { iName = rs.getString("insumo_name"); } catch (SQLException ignored) {}
        String iUnit = null;
        try { iUnit = rs.getString("insumo_unit"); } catch (SQLException ignored) {}
        return new PurchaseItem(
                rs.getObject("id", UUID.class),
                rs.getObject("purchase_order_id", UUID.class),
                rs.getObject("insumo_id", UUID.class),
                iName,
                iUnit,
                rs.getBigDecimal("quantity"),
                rs.getBigDecimal("unit_cost")
        );
    }

    public BigDecimal subtotal() {
        return quantity.multiply(unitCost);
    }
}
