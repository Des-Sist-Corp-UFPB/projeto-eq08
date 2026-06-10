package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Escala/turno de trabalho de um colaborador.
 */
public record EmployeeSchedule(
        UUID id,
        UUID tenantId,
        UUID userId,
        String userName,
        LocalDate shiftDate,
        String startTime,
        String endTime,
        String notes
) {
    public static EmployeeSchedule fromResultSet(ResultSet rs) throws SQLException {
        String uName = null;
        try { uName = rs.getString("user_name"); } catch (SQLException ignored) {}
        return new EmployeeSchedule(
                rs.getObject("id", UUID.class),
                rs.getObject("tenant_id", UUID.class),
                rs.getObject("user_id", UUID.class),
                uName,
                rs.getObject("shift_date", LocalDate.class),
                rs.getString("start_time"),
                rs.getString("end_time"),
                rs.getString("notes")
        );
    }
}

