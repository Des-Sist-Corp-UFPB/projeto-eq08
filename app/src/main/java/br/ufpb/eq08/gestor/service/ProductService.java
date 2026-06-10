package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.Product;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.*;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductService {
    private final ProductRepository productRepo;
    private final InsumoRepository insumoRepo;
    private final CategoryRepository catRepo;
    private final AuditRepository auditRepo;
    private final DataSource ds;

    public ProductService(ProductRepository productRepo, InsumoRepository insumoRepo,
                          CategoryRepository catRepo, AuditRepository auditRepo, DataSource ds) {
        this.productRepo = productRepo; this.insumoRepo = insumoRepo;
        this.catRepo = catRepo; this.auditRepo = auditRepo; this.ds = ds;
    }

    public List<Product> list(UUID tenantId) { return productRepo.findByTenant(tenantId); }

    public Product getById(UUID id, UUID tenantId) {
        return productRepo.findById(id)
                .filter(p -> p.tenantId().equals(tenantId))
                .orElseThrow(() -> AppException.notFound("Produto não encontrado."));
    }

    public Product create(UUID tenantId, UUID userId, UUID categoryId, String name, BigDecimal price,
                          List<UUID> ingredientIds, List<BigDecimal> ingredientQtys) {
        // Validar ingredientes
        for (UUID ingId : ingredientIds) {
            insumoRepo.findById(ingId)
                    .filter(i -> i.tenantId().equals(tenantId))
                    .orElseThrow(() -> AppException.badRequest("Insumo " + ingId + " inválido."));
        }

        Product product = productRepo.create(tenantId, categoryId, name, price, ingredientIds, ingredientQtys);
        auditRepo.log(tenantId, userId, "PRODUCT_CREATE", "products", product.id().toString(),
                null, Map.of("name", name, "price", price, "ingredients", ingredientIds.size()), null);
        return product;
    }

    public void delete(UUID id, UUID tenantId, UUID userId) {
        Product p = getById(id, tenantId);
        auditRepo.log(tenantId, userId, "PRODUCT_DELETE", "products", id.toString(),
                Map.of("name", p.name()), null, null);
        productRepo.delete(id);
    }
}
