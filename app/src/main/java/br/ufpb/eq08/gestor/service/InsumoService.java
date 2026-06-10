package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.Insumo;
import br.ufpb.eq08.gestor.domain.StockMovement;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.InsumoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Serviço de Insumos: CRUD e movimentações de estoque.
 */
public class InsumoService {

    private static final Logger log = LoggerFactory.getLogger(InsumoService.class);

    private final InsumoRepository insumoRepo;
    private final AuditRepository auditRepo;
    private final DataSource ds;

    public InsumoService(InsumoRepository insumoRepo, AuditRepository auditRepo, DataSource ds) {
        this.insumoRepo = insumoRepo;
        this.auditRepo  = auditRepo;
        this.ds         = ds;
    }

    public List<Insumo> list(UUID tenantId) {
        return insumoRepo.findByTenant(tenantId);
    }

    public Insumo getById(UUID id, UUID tenantId) {
        return insumoRepo.findById(id)
                .filter(i -> i.tenantId().equals(tenantId))
                .orElseThrow(() -> AppException.notFound("Insumo não encontrado."));
    }

    public Insumo create(UUID tenantId, UUID userId, UUID categoryId, String name, String unit,
                          BigDecimal currentStock, BigDecimal minimumStock, BigDecimal unitCost) {
        Insumo insumo = insumoRepo.create(tenantId, categoryId, name, unit, currentStock, minimumStock, unitCost);

        auditRepo.log(tenantId, userId, "INSUMO_CREATE", "insumos", insumo.id().toString(),
                null, Map.of("name", name, "unit", unit, "current_stock", currentStock), null);

        // Registrar movimentação inicial se estoque > 0
        if (currentStock.compareTo(BigDecimal.ZERO) > 0) {
            try (Connection conn = ds.getConnection()) {
                insumoRepo.insertMovement(conn, tenantId, insumo.id(), currentStock,
                        "INPUT", "Estoque inicial no cadastro", userId, null);
            } catch (SQLException e) {
                log.warn("Erro ao registrar movimentação inicial: {}", e.getMessage());
            }
        }

        return insumo;
    }

    public Insumo update(UUID id, UUID tenantId, UUID userId, UUID categoryId, String name,
                          String unit, BigDecimal minimumStock) {
        Insumo existing = getById(id, tenantId);
        Insumo updated = insumoRepo.update(id, categoryId, name, unit, minimumStock);

        auditRepo.log(tenantId, userId, "INSUMO_UPDATE", "insumos", id.toString(),
                Map.of("name", existing.name(), "minimum_stock", existing.minimumStock()),
                Map.of("name", updated.name(), "minimum_stock", updated.minimumStock()), null);

        return updated;
    }

    public void delete(UUID id, UUID tenantId, UUID userId) {
        Insumo insumo = getById(id, tenantId);
        insumoRepo.delete(id);
        auditRepo.log(tenantId, userId, "INSUMO_DELETE", "insumos", id.toString(),
                Map.of("name", insumo.name()), null, null);
    }

    /**
     * Lança movimentação manual de estoque (INPUT / OUTPUT / ADJUSTMENT).
     */
    public void addMovement(UUID insumoId, UUID tenantId, UUID userId,
                             BigDecimal quantity, String type, String reason) {
        Insumo insumo = getById(insumoId, tenantId);

        // Calcular novo estoque
        BigDecimal delta = type.equals("OUTPUT") ? quantity.negate() : quantity;
        BigDecimal newStock = insumo.currentStock().add(delta);

        if (newStock.compareTo(BigDecimal.ZERO) < 0) {
            throw AppException.badRequest("Estoque insuficiente. Disponível: " +
                    insumo.currentStock() + " " + insumo.unit());
        }

        // Novo custo médio (só atualiza em INPUT)
        BigDecimal newCost = insumo.unitCost();
        if ("INPUT".equals(type)) {
            // Mantém custo atual para movimentações manuais
            newCost = insumo.unitCost();
        }

        try (Connection conn = ds.getConnection()) {
            insumoRepo.updateStockAndCost(conn, insumoId, newStock, newCost);
            insumoRepo.insertMovement(conn, tenantId, insumoId, delta, type, reason, userId, null);
        } catch (SQLException e) {
            log.error("Erro ao registrar movimentação: {}", e.getMessage());
            throw new RuntimeException("Erro ao registrar movimentação de estoque.", e);
        }

        log.info("Movimentação {} de {} {} no insumo {}", type, quantity, insumo.unit(), insumo.name());
    }

    public List<StockMovement> listMovements(UUID tenantId, int limit) {
        return insumoRepo.findMovementsByTenant(tenantId, limit);
    }

    public List<Insumo> listBelowMinimum(UUID tenantId) {
        return insumoRepo.findBelowMinimum(tenantId);
    }
}
