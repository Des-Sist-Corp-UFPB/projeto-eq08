package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Recomendação de IA (reabastecimento de estoque ou otimização de escalas).
 * type: STOCK_REPLENISHMENT | SHIFT_OPTIMIZATION
 * impactLevel: HIGH | MEDIUM | LOW
 * status: PENDING | APPLIED | DISMISSED
 */
public record AIRecommendation(
        UUID id,
        UUID tenantId,
        String type,
        String title,
        String description,
        String impactLevel,
        String actionData,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static AIRecommendation fromResultSet(ResultSet rs) throws SQLException {
        return new AIRecommendation(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getString("type"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("impact_level"),
                rs.getString("action_data"),
                rs.getString("status"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
