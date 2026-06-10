package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.Absence;
import br.ufpb.eq08.gestor.domain.EmployeeSchedule;
import br.ufpb.eq08.gestor.domain.ShiftTrade;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para Escalas, Trocas de Turno e Afastamentos.
 */
public class ScheduleRepository {

    private static final Logger log = LoggerFactory.getLogger(ScheduleRepository.class);
    private final DataSource ds;

    public ScheduleRepository(DataSource ds) {
        this.ds = ds;
    }

    // ========================================================================
    // EMPLOYEE SCHEDULES
    // ========================================================================

    public List<EmployeeSchedule> findByTenant(UUID tenantId, LocalDate from, LocalDate to) {
        String sql = """
                SELECT es.*, u.name AS user_name
                FROM employee_schedules es
                JOIN users u ON es.user_id = u.id
                WHERE es.tenant_id = ?
                  AND (? IS NULL OR es.shift_date >= ?)
                  AND (? IS NULL OR es.shift_date <= ?)
                ORDER BY es.shift_date, es.start_time
                """;
        List<EmployeeSchedule> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, from);
            stmt.setObject(3, from);
            stmt.setObject(4, to);
            stmt.setObject(5, to);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(EmployeeSchedule.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar escalas: {}", e.getMessage());
        }
        return list;
    }

    public Optional<EmployeeSchedule> findScheduleById(UUID id) {
        String sql = "SELECT es.*, u.name AS user_name FROM employee_schedules es JOIN users u ON es.user_id = u.id WHERE es.id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(EmployeeSchedule.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar escala: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public EmployeeSchedule createSchedule(UUID tenantId, UUID userId, LocalDate shiftDate,
                                            String startTime, String endTime, String notes) {
        String sql = """
                INSERT INTO employee_schedules (tenant_id, user_id, shift_date, start_time, end_time, notes)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, userId);
            stmt.setObject(3, shiftDate);
            stmt.setString(4, startTime);
            stmt.setString(5, endTime);
            stmt.setString(6, notes);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return EmployeeSchedule.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao criar escala: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar escala.", e);
        }
        throw new RuntimeException("Erro inesperado.");
    }

    public void deleteSchedule(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM employee_schedules WHERE id = ?")) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao excluir escala: {}", e.getMessage());
            throw new RuntimeException("Erro ao excluir escala.", e);
        }
    }

    // ========================================================================
    // SHIFT TRADES
    // ========================================================================

    public List<ShiftTrade> findTradesByTenant(UUID tenantId) {
        String sql = """
                SELECT st.*,
                       ru.name AS requesting_user_name,
                       tu.name AS target_user_name,
                       au.name AS approved_by_name
                FROM shift_trades st
                JOIN users ru ON st.requesting_user_id = ru.id
                LEFT JOIN users tu ON st.target_user_id = tu.id
                LEFT JOIN users au ON st.approved_by_id = au.id
                WHERE st.tenant_id = ?
                ORDER BY st.created_at DESC
                """;
        List<ShiftTrade> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(ShiftTrade.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar trocas: {}", e.getMessage());
        }
        return list;
    }

    public ShiftTrade createTrade(UUID tenantId, UUID requestingUserId, UUID targetUserId,
                                   UUID requestingScheduleId, UUID targetScheduleId) {
        String sql = """
                INSERT INTO shift_trades (tenant_id, requesting_user_id, target_user_id, requesting_schedule_id, target_schedule_id, status)
                VALUES (?, ?, ?, ?, ?, 'PENDING') RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, requestingUserId);
            stmt.setObject(3, targetUserId);
            stmt.setObject(4, requestingScheduleId);
            stmt.setObject(5, targetScheduleId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return ShiftTrade.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao criar troca: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar solicitação de troca.", e);
        }
        throw new RuntimeException("Erro inesperado.");
    }

    /**
     * Aprova uma troca: executa o swap atômico de user_id entre as duas escalas.
     */
    public void approveTrade(UUID tradeId, UUID approvedById) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Buscar trade
                ShiftTrade trade;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT * FROM shift_trades WHERE id = ? FOR UPDATE")) {
                    stmt.setObject(1, tradeId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) throw new RuntimeException("Troca não encontrada.");
                    trade = ShiftTrade.fromResultSet(rs);
                }

                if (!"PENDING".equals(trade.status())) {
                    throw new RuntimeException("Esta troca já foi processada.");
                }

                // Atomic swap: trocar user_ids entre as escalas
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE employee_schedules SET user_id = ? WHERE id = ?")) {
                    stmt.setObject(1, trade.targetUserId());
                    stmt.setObject(2, trade.requestingScheduleId());
                    stmt.executeUpdate();
                    stmt.setObject(1, trade.requestingUserId());
                    stmt.setObject(2, trade.targetScheduleId());
                    stmt.executeUpdate();
                }

                // Atualizar status da troca
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE shift_trades SET status = 'APPROVED', approved_by_id = ?, updated_at = NOW() WHERE id = ?")) {
                    stmt.setObject(1, approvedById);
                    stmt.setObject(2, tradeId);
                    stmt.executeUpdate();
                }

                conn.commit();
                log.info("Troca #{} aprovada.", tradeId.toString().substring(0, 8));
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Erro ao aprovar troca: {}", e.getMessage());
            throw new RuntimeException("Erro ao aprovar troca.", e);
        }
    }

    public void rejectTrade(UUID tradeId, UUID approvedById) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE shift_trades SET status = 'REJECTED', approved_by_id = ?, updated_at = NOW() WHERE id = ?")) {
            stmt.setObject(1, approvedById);
            stmt.setObject(2, tradeId);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao rejeitar troca: {}", e.getMessage());
            throw new RuntimeException("Erro ao rejeitar troca.", e);
        }
    }

    // ========================================================================
    // ABSENCES
    // ========================================================================

    public List<Absence> findAbsencesByTenant(UUID tenantId) {
        String sql = """
                SELECT a.*, u.name AS user_name, au.name AS approved_by_name
                FROM absences a
                JOIN users u ON a.user_id = u.id
                LEFT JOIN users au ON a.approved_by_id = au.id
                WHERE a.tenant_id = ?
                ORDER BY a.start_date DESC
                """;
        List<Absence> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(Absence.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar afastamentos: {}", e.getMessage());
        }
        return list;
    }

    public Absence createAbsence(UUID tenantId, UUID userId, LocalDate startDate, LocalDate endDate,
                                  String type, String reason, boolean isManager) {
        String initialStatus = isManager ? "APPROVED" : "PENDING";
        String sql = """
                INSERT INTO absences (tenant_id, user_id, start_date, end_date, type, reason, status)
                VALUES (?, ?, ?, ?, ?, ?, ?) RETURNING *
                """;
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Absence absence;
                try (PreparedStatement stmt = conn.prepareStatement(sql)) {
                    stmt.setObject(1, tenantId);
                    stmt.setObject(2, userId);
                    stmt.setObject(3, startDate);
                    stmt.setObject(4, endDate);
                    stmt.setString(5, type);
                    stmt.setString(6, reason);
                    stmt.setString(7, initialStatus);
                    ResultSet rs = stmt.executeQuery();
                    rs.next();
                    absence = Absence.fromResultSet(rs);
                }

                // Se já aprovado (gestor), remover escalas conflitantes
                if ("APPROVED".equals(initialStatus)) {
                    deleteConflictingSchedules(conn, tenantId, userId, startDate, endDate);
                }

                conn.commit();
                return absence;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Erro ao criar afastamento: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar afastamento.", e);
        }
    }

    public Absence approveAbsence(UUID absenceId, UUID approvedById) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                Absence absence;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE absences SET status = 'APPROVED', approved_by_id = ? WHERE id = ? RETURNING *")) {
                    stmt.setObject(1, approvedById);
                    stmt.setObject(2, absenceId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) throw new RuntimeException("Afastamento não encontrado.");
                    absence = Absence.fromResultSet(rs);
                }

                // Remover escalas conflitantes
                deleteConflictingSchedules(conn, absence.tenantId(), absence.userId(),
                        absence.startDate(), absence.endDate());

                conn.commit();
                return absence;
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Erro ao aprovar afastamento: {}", e.getMessage());
            throw new RuntimeException("Erro ao aprovar afastamento.", e);
        }
    }

    private void deleteConflictingSchedules(Connection conn, UUID tenantId, UUID userId,
                                             LocalDate start, LocalDate end) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(
                "DELETE FROM employee_schedules WHERE tenant_id = ? AND user_id = ? AND shift_date BETWEEN ? AND ?")) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, userId);
            stmt.setObject(3, start);
            stmt.setObject(4, end);
            int deleted = stmt.executeUpdate();
            if (deleted > 0) log.info("{} escalas conflitantes removidas para afastamento.", deleted);
        }
    }
}
