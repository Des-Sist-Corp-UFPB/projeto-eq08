package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Categoria de insumos ou produtos.
 * type = 'INSUMO' | 'PRODUCT'
 */
public record Category(
        UUID id,
        UUID tenantId,
        String name,
        String type,
        OffsetDateTime createdAt
) {
    public static Category fromResultSet(ResultSet rs) throws SQLException {
        return new Category(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("name"),
                rs.getString("type"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}
