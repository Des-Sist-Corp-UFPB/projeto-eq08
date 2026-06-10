package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.PurchaseOrder;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.SupplierRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PurchaseService {
    private final SupplierRepository repo;
    private final AuditRepository auditRepo;

    public PurchaseService(SupplierRepository repo, AuditRepository auditRepo) {
        this.repo = repo; this.auditRepo = auditRepo;
    }

    public List<PurchaseOrder> list(UUID tenantId) { return repo.findOrdersByTenant(tenantId); }

    public PurchaseOrder getById(UUID id, UUID tenantId) {
        return repo.findOrderById(id).filter(p -> p.tenantId().equals(tenantId))
                .orElseThrow(() -> AppException.notFound("Ordem de compra não encontrada."));
    }

    public PurchaseOrder create(UUID tenantId, UUID userId, UUID supplierId,
                                List<UUID> insumoIds, List<BigDecimal> quantities, List<BigDecimal> unitCosts) {
        PurchaseOrder po = repo.createOrder(tenantId, supplierId, insumoIds, quantities, unitCosts);
        auditRepo.log(tenantId, userId, "PURCHASE_CREATE", "purchase_orders", po.id().toString(),
                null, Map.of("supplier_id", supplierId.toString(), "total", po.totalPrice()), null);
        return po;
    }

    public PurchaseOrder complete(UUID id, UUID tenantId, UUID userId,
                                  Integer deliveryDays, Integer qualityRating, Integer priceRating) {
        PurchaseOrder po = repo.completeOrder(id, tenantId, userId, deliveryDays, qualityRating, priceRating);
        auditRepo.log(tenantId, userId, "PURCHASE_COMPLETE", "purchase_orders", id.toString(),
                null, Map.of("status", "COMPLETED", "delivery_days", deliveryDays != null ? deliveryDays : "N/A"), null);
        return po;
    }

    public void cancel(UUID id, UUID tenantId, UUID userId) {
        getById(id, tenantId);
        repo.cancelOrder(id);
        auditRepo.log(tenantId, userId, "PURCHASE_CANCEL", "purchase_orders", id.toString(),
                null, Map.of("status", "CANCELLED"), null);
    }
}
