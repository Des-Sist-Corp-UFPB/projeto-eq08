package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Fornecedor comercial parceiro.
 */
public record Supplier(
        UUID id,
        UUID tenantId,
        String name,
        String document,
        String phone,
        String email,
        String contactName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Supplier fromResultSet(ResultSet rs) throws SQLException {
        return new Supplier(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("name"),
                rs.getString("document"),
                rs.getString("phone"),
                rs.getString("email"),
                rs.getString("contact_name"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}

