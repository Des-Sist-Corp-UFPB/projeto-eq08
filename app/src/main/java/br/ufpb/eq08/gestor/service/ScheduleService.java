package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.Absence;
import br.ufpb.eq08.gestor.domain.EmployeeSchedule;
import br.ufpb.eq08.gestor.domain.ShiftTrade;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.ScheduleRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ScheduleService {
    private final ScheduleRepository repo;
    private final AuditRepository auditRepo;

    public ScheduleService(ScheduleRepository repo, AuditRepository auditRepo) {
        this.repo = repo; this.auditRepo = auditRepo;
    }

    public List<EmployeeSchedule> listSchedules(UUID tenantId, LocalDate from, LocalDate to) {
        return repo.findByTenant(tenantId, from, to);
    }

    public EmployeeSchedule createSchedule(UUID tenantId, UUID userId, UUID operatorId,
                                            LocalDate shiftDate, String startTime, String endTime, String notes) {
        EmployeeSchedule s = repo.createSchedule(tenantId, operatorId, shiftDate, startTime, endTime, notes);
        auditRepo.log(tenantId, userId, "SCHEDULE_CREATE", "employee_schedules", s.id().toString(),
                null, Map.of("shift_date", shiftDate.toString(), "user_id", operatorId.toString()), null);
        return s;
    }

    public void deleteSchedule(UUID id, UUID tenantId, UUID userId) {
        repo.findScheduleById(id).filter(s -> s.tenantId().equals(tenantId))
                .orElseThrow(() -> AppException.notFound("Escala não encontrada."));
        repo.deleteSchedule(id);
        auditRepo.log(tenantId, userId, "SCHEDULE_DELETE", "employee_schedules", id.toString(), null, null, null);
    }

    public List<ShiftTrade> listTrades(UUID tenantId) { return repo.findTradesByTenant(tenantId); }

    public ShiftTrade createTrade(UUID tenantId, UUID requestingUserId, UUID targetUserId,
                                   UUID requestingScheduleId, UUID targetScheduleId) {
        return repo.createTrade(tenantId, requestingUserId, targetUserId, requestingScheduleId, targetScheduleId);
    }

    public void approveTrade(UUID tradeId, UUID tenantId, UUID approvedById) {
        repo.approveTrade(tradeId, approvedById);
        auditRepo.log(tenantId, approvedById, "TRADE_APPROVE", "shift_trades", tradeId.toString(), null,
                Map.of("status", "APPROVED"), null);
    }

    public void rejectTrade(UUID tradeId, UUID tenantId, UUID approvedById) {
        repo.rejectTrade(tradeId, approvedById);
        auditRepo.log(tenantId, approvedById, "TRADE_REJECT", "shift_trades", tradeId.toString(), null,
                Map.of("status", "REJECTED"), null);
    }

    public List<Absence> listAbsences(UUID tenantId) { return repo.findAbsencesByTenant(tenantId); }

    public Absence createAbsence(UUID tenantId, UUID userId, UUID currentUserId, String currentRole,
                                  LocalDate startDate, LocalDate endDate, String type, String reason) {
        boolean isManager = List.of("OWNER", "MANAGER", "SUPERVISOR").contains(currentRole);
        return repo.createAbsence(tenantId, userId, startDate, endDate, type, reason, isManager);
    }

    public Absence approveAbsence(UUID absenceId, UUID tenantId, UUID approvedById) {
        return repo.approveAbsence(absenceId, approvedById);
    }
}
