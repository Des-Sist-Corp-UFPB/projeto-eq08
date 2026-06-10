package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.Category;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.CategoryRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

public class CategoryService {
    private final CategoryRepository repo;
    private final AuditRepository auditRepo;

    public CategoryService(CategoryRepository repo, AuditRepository auditRepo) {
        this.repo = repo; this.auditRepo = auditRepo;
    }

    public List<Category> list(UUID tenantId, String type) { return repo.findByTenant(tenantId, type); }

    public Category getById(UUID id) {
        return repo.findById(id).orElseThrow(() -> AppException.notFound("Categoria não encontrada."));
    }

    public Category create(UUID tenantId, UUID userId, String name, String type) {
        Category c = repo.create(tenantId, name, type);
        auditRepo.log(tenantId, userId, "CATEGORY_CREATE", "categories", c.id().toString(),
                null, Map.of("name", name, "type", type), null);
        return c;
    }

    public void delete(UUID id, UUID tenantId, UUID userId) {
        Category c = getById(id);
        if (!c.tenantId().equals(tenantId)) throw AppException.notFound("Categoria não encontrada.");
        auditRepo.log(tenantId, userId, "CATEGORY_DELETE", "categories", id.toString(),
                Map.of("name", c.name()), null, null);
        repo.delete(id);
    }
}
