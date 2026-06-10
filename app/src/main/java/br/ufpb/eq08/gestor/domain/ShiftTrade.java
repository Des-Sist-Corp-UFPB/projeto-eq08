package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Solicitação de troca de turno entre colaboradores.
 */
public record ShiftTrade(
        UUID id,
        UUID tenantId,
        UUID requestingUserId,
        String requestingUserName,
        UUID targetUserId,
        String targetUserName,
        UUID requestingScheduleId,
        UUID targetScheduleId,
        String status,
        UUID approvedById,
        String approvedByName,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static ShiftTrade fromResultSet(ResultSet rs) throws SQLException {
        String reqName = null;
        try { reqName = rs.getString("requesting_user_name"); } catch (SQLException ignored) {}
        String tgtName = null;
        try { tgtName = rs.getString("target_user_name"); } catch (SQLException ignored) {}
        String appName = null;
        try { appName = rs.getString("approved_by_name"); } catch (SQLException ignored) {}
        return new ShiftTrade(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("requesting_user_id", UUID.class),
                reqName,
                rs.getObject("target_user_id", UUID.class),
                tgtName,
                rs.getObject("requesting_schedule_id", UUID.class),
                rs.getObject("target_schedule_id", UUID.class),
                rs.getString("status"),
                rs.getObject("approved_by_id", UUID.class),
                appName,
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }
}
