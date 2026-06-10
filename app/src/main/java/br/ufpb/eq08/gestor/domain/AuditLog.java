package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Log de auditoria de operações críticas.
 */
public record AuditLog(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userName,
        String action,
        String tableName,
        String recordId,
        String beforeState,
        String afterState,
        String ipAddress,
        OffsetDateTime createdAt
) {
    public static AuditLog fromResultSet(ResultSet rs) throws SQLException {
        String uName = null;
        try { uName = rs.getString("user_name"); } catch (SQLException ignored) {}
        return new AuditLog(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                uName,
                rs.getString("action"),
                rs.getString("table_name"),
                rs.getString("record_id"),
                rs.getString("before_state"),
                rs.getString("after_state"),
                rs.getString("ip_address"),
                rs.getObject("created_at", OffsetDateTime.class)
        );
    }
}
