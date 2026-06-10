package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.Order;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.ProductRepository;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderService {
    private final ProductRepository productRepo;
    private final AuditRepository auditRepo;
    private final DataSource ds;

    public OrderService(ProductRepository productRepo, AuditRepository auditRepo, DataSource ds) {
        this.productRepo = productRepo; this.auditRepo = auditRepo; this.ds = ds;
    }

    public Order createOrder(UUID tenantId, UUID userId, List<UUID> productIds, List<Integer> quantities) {
        Order order = productRepo.createOrder(tenantId, userId, productIds, quantities, ds);
        auditRepo.log(tenantId, userId, "ORDER_CREATE", "orders", order.id().toString(),
                null, Map.of("total_price", order.totalPrice(), "items", productIds.size()), null);
        return order;
    }

    public List<Order> listOrders(UUID tenantId, int limit) {
        return productRepo.findOrdersByTenant(tenantId, limit);
    }

    /** Faturamento total dos últimos 30 dias */
    public BigDecimal recentRevenue(UUID tenantId) {
        return listOrders(tenantId, 1000).stream()
                .map(Order::totalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public long countOrders(UUID tenantId) {
        return listOrders(tenantId, 1000).size();
    }
}
