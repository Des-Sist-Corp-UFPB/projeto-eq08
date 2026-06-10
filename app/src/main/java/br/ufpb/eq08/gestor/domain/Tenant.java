package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa uma empresa (tenant) no ecossistema SaaS.
 * Imutável — usa Java Record.
 */
public record Tenant(
        UUID id,
        String name,
        String slug,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static Tenant fromResultSet(ResultSet rs) throws SQLException {
        return new Tenant(
                rs.getObject("id", UUID.class),
                rs.getString("name"),
                rs.getString("slug"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    public boolean isActive() {
        return "active".equals(status);
    }
}
