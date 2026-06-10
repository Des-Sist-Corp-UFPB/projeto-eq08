package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Afastamento, férias ou licença de colaborador.
 */
public record Absence(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userName,
        LocalDate startDate,
        LocalDate endDate,
        String type,
        String reason,
        String status,
        UUID approvedById,
        String approvedByName
) {
    public static Absence fromResultSet(ResultSet rs) throws SQLException {
        String uName = null;
        try { uName = rs.getString("user_name"); } catch (SQLException ignored) {}
        String aName = null;
        try { aName = rs.getString("approved_by_name"); } catch (SQLException ignored) {}
        return new Absence(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                uName,
                rs.getObject("start_date", LocalDate.class),
                rs.getObject("end_date", LocalDate.class),
                rs.getString("type"),
                rs.getString("reason"),
                rs.getString("status"),
                rs.getObject("approved_by_id", UUID.class),
                aName
        );
    }
}
