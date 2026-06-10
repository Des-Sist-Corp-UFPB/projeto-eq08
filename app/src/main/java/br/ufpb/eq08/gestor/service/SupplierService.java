package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.Supplier;
import br.ufpb.eq08.gestor.domain.SupplierPerformance;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.SupplierRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SupplierService {
    private final SupplierRepository repo;
    private final AuditRepository auditRepo;

    public SupplierService(SupplierRepository repo, AuditRepository auditRepo) {
        this.repo = repo; this.auditRepo = auditRepo;
    }

    public List<Supplier> list(UUID tenantId) { return repo.findByTenant(tenantId); }

    public Supplier getById(UUID id, UUID tenantId) {
        return repo.findById(id).filter(s -> s.tenantId().equals(tenantId))
                .orElseThrow(() -> AppException.notFound("Fornecedor não encontrado."));
    }

    public Supplier create(UUID tenantId, UUID userId, String name, String document,
                           String phone, String email, String contactName) {
        Supplier s = repo.create(tenantId, name, document, phone, email, contactName);
        auditRepo.log(tenantId, userId, "SUPPLIER_CREATE", "suppliers", s.id().toString(),
                null, Map.of("name", name), null);
        return s;
    }

    public Supplier update(UUID id, UUID tenantId, UUID userId, String name, String document,
                           String phone, String email, String contactName) {
        getById(id, tenantId);
        Supplier updated = repo.update(id, name, document, phone, email, contactName);
        auditRepo.log(tenantId, userId, "SUPPLIER_UPDATE", "suppliers", id.toString(),
                null, Map.of("name", updated.name()), null);
        return updated;
    }

    public void delete(UUID id, UUID tenantId, UUID userId) {
        Supplier s = getById(id, tenantId);
        auditRepo.log(tenantId, userId, "SUPPLIER_DELETE", "suppliers", id.toString(),
                Map.of("name", s.name()), null, null);
        repo.delete(id);
    }

    public SupplierPerformance getPerformance(UUID id, UUID tenantId) {
        getById(id, tenantId);
        return repo.getPerformance(id).orElseThrow(() -> AppException.notFound("Fornecedor não encontrado."));
    }
}
