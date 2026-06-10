package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.AuditLog;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Repositório para Logs de Auditoria.
 */
public class AuditRepository {

    private static final Logger log = LoggerFactory.getLogger(AuditRepository.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private final DataSource ds;

    public AuditRepository(DataSource ds) {
        this.ds = ds;
    }

    public void log(UUID tenantId, UUID userId, String action, String tableName,
                    String recordId, Map<String, Object> beforeState, Map<String, Object> afterState,
                    String ipAddress) {
        String sql = """
                INSERT INTO audit_logs (tenant_id, user_id, action, table_name, record_id, before_state, after_state, ip_address)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, userId);
            stmt.setString(3, action);
            stmt.setString(4, tableName);
            stmt.setString(5, recordId);
            stmt.setObject(6, toJsonb(beforeState));
            stmt.setObject(7, toJsonb(afterState));
            stmt.setString(8, ipAddress);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao registrar audit log: {}", e.getMessage());
        }
    }

    public List<AuditLog> findByTenant(UUID tenantId, int limit) {
        String sql = """
                SELECT al.*, u.name AS user_name
                FROM audit_logs al
                LEFT JOIN users u ON al.user_id = u.id
                WHERE al.tenant_id = ?
                ORDER BY al.created_at DESC
                LIMIT ?
                """;
        List<AuditLog> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(AuditLog.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar audit logs: {}", e.getMessage());
        }
        return list;
    }

    private PGobject toJsonb(Map<String, Object> data) throws SQLException {
        if (data == null) return null;
        PGobject obj = new PGobject();
        obj.setType("jsonb");
        try {
            obj.setValue(mapper.writeValueAsString(data));
        } catch (JsonProcessingException e) {
            obj.setValue("{}");
        }
        return obj;
    }
}
