package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Previsão de demanda gerada pelo motor de IA simplificado.
 */
public record DemandForecast(
        UUID id,
        UUID tenantId,
        LocalDate targetDate,
        int predictedOrders,
        BigDecimal predictedRevenue,
        BigDecimal confidenceScore,
        String modelVersion,
        OffsetDateTime createdAt
) {
    public static DemandForecast fromResultSet(ResultSet rs) throws SQLException {
        return new DemandForecast(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("target_date", LocalDate.class),
                rs.getInt("predicted_orders"),
                rs.getBigDecimal("predicted_revenue"),
                rs.getBigDecimal("confidence_score"),
                rs.getString("model_version"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}

// ──────────────────────────────────────────────────────────────────────────────

/**
 * Recomendação de IA (reabastecimento de estoque ou otimização de escalas).
 * type: STOCK_REPLENISHMENT | SHIFT_OPTIMIZATION
 * impactLevel: HIGH | MEDIUM | LOW
 * status: PENDING | APPLIED | DISMISSED
 */
record AIRecommendation(
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
