package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.PurchaseItem;
import br.ufpb.eq08.gestor.domain.PurchaseOrder;
import br.ufpb.eq08.gestor.domain.Supplier;
import br.ufpb.eq08.gestor.domain.SupplierPerformance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para Fornecedores e Ordens de Compra.
 * Inclui o Motor de Abastecimento (atualização de estoque + custo médio ao concluir compra).
 */
public class SupplierRepository {

    private static final Logger log = LoggerFactory.getLogger(SupplierRepository.class);
    private final DataSource ds;

    public SupplierRepository(DataSource ds) {
        this.ds = ds;
    }

    // =====================================================================
    // SUPPLIERS
    // =====================================================================

    public List<Supplier> findByTenant(UUID tenantId) {
        String sql = "SELECT * FROM suppliers WHERE tenant_id = ? ORDER BY name";
        List<Supplier> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(Supplier.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar fornecedores: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Supplier> findById(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM suppliers WHERE id = ?")) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(Supplier.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar fornecedor: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Supplier create(UUID tenantId, String name, String document,
                           String phone, String email, String contactName) {
        String sql = """
                INSERT INTO suppliers (tenant_id, name, document, phone, email, contact_name)
                VALUES (?, ?, ?, ?, ?, ?) RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setString(2, name);
            stmt.setString(3, document);
            stmt.setString(4, phone);
            stmt.setString(5, email);
            stmt.setString(6, contactName);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Supplier.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao criar fornecedor: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar fornecedor.", e);
        }
        throw new RuntimeException("Erro inesperado.");
    }

    public Supplier update(UUID id, String name, String document, String phone, String email, String contactName) {
        String sql = """
                UPDATE suppliers
                SET name = COALESCE(?, name),
                    document = COALESCE(?, document),
                    phone = COALESCE(?, phone),
                    email = COALESCE(?, email),
                    contact_name = COALESCE(?, contact_name),
                    updated_at = NOW()
                WHERE id = ? RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, document);
            stmt.setString(3, phone);
            stmt.setString(4, email);
            stmt.setString(5, contactName);
            stmt.setObject(6, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Supplier.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao atualizar fornecedor: {}", e.getMessage());
            throw new RuntimeException("Erro ao atualizar fornecedor.", e);
        }
        throw new RuntimeException("Fornecedor não encontrado.");
    }

    public void delete(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM suppliers WHERE id = ?")) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao excluir fornecedor: {}", e.getMessage());
            throw new RuntimeException("Erro ao excluir fornecedor.", e);
        }
    }

    public Optional<SupplierPerformance> getPerformance(UUID supplierId) {
        String sql = """
                SELECT
                    s.id AS supplier_id,
                    s.name AS supplier_name,
                    COALESCE(AVG(po.delivery_days), 0)     AS avg_delivery_days,
                    COALESCE(AVG(po.quality_rating), 0)    AS avg_quality_rating,
                    COALESCE(AVG(po.price_rating), 0)      AS avg_price_rating,
                    COALESCE(SUM(po.total_price), 0)       AS total_purchases_value,
                    COUNT(po.id)                           AS purchase_orders_count
                FROM suppliers s
                LEFT JOIN purchase_orders po ON s.id = po.supplier_id AND po.status = 'COMPLETED'
                WHERE s.id = ?
                GROUP BY s.id, s.name
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, supplierId);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return Optional.of(new SupplierPerformance(
                        rs.getObject("supplier_id", UUID.class),
                        rs.getString("supplier_name"),
                        rs.getDouble("avg_delivery_days"),
                        rs.getDouble("avg_quality_rating"),
                        rs.getDouble("avg_price_rating"),
                        rs.getBigDecimal("total_purchases_value"),
                        rs.getLong("purchase_orders_count")
                ));
            }
        } catch (SQLException e) {
            log.error("Erro ao calcular performance de fornecedor: {}", e.getMessage());
        }
        return Optional.empty();
    }

    // =====================================================================
    // PURCHASE ORDERS + MOTOR DE ABASTECIMENTO
    // =====================================================================

    public List<PurchaseOrder> findOrdersByTenant(UUID tenantId) {
        String sql = """
                SELECT po.*, s.name AS supplier_name
                FROM purchase_orders po
                JOIN suppliers s ON po.supplier_id = s.id
                WHERE po.tenant_id = ?
                ORDER BY po.created_at DESC
                """;
        List<PurchaseOrder> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                PurchaseOrder po = PurchaseOrder.fromResultSet(rs);
                list.add(loadPurchaseItems(conn, po));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar ordens de compra: {}", e.getMessage());
        }
        return list;
    }

    public Optional<PurchaseOrder> findOrderById(UUID id) {
        String sql = """
                SELECT po.*, s.name AS supplier_name
                FROM purchase_orders po
                JOIN suppliers s ON po.supplier_id = s.id
                WHERE po.id = ?
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                PurchaseOrder po = PurchaseOrder.fromResultSet(rs);
                return Optional.of(loadPurchaseItems(conn, po));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar ordem de compra: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private PurchaseOrder loadPurchaseItems(Connection conn, PurchaseOrder po) throws SQLException {
        String sql = """
                SELECT pi.*, i.name AS insumo_name, i.unit AS insumo_unit
                FROM purchase_items pi
                JOIN insumos i ON pi.insumo_id = i.id
                WHERE pi.purchase_order_id = ?
                """;
        List<PurchaseItem> items = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, po.id());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) items.add(PurchaseItem.fromResultSet(rs));
        }
        return new PurchaseOrder(po.id(), po.tenantId(), po.supplierId(), po.supplierName(),
                po.status(), po.totalPrice(), po.deliveryDays(), po.qualityRating(), po.priceRating(),
                items, po.createdAt(), po.updatedAt());
    }

    public PurchaseOrder createOrder(UUID tenantId, UUID supplierId,
                                     List<UUID> insumoIds, List<BigDecimal> quantities, List<BigDecimal> unitCosts) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                BigDecimal total = BigDecimal.ZERO;
                for (int i = 0; i < quantities.size(); i++) {
                    total = total.add(quantities.get(i).multiply(unitCosts.get(i)));
                }

                String insertPO = """
                        INSERT INTO purchase_orders (tenant_id, supplier_id, status, total_price)
                        VALUES (?, ?, 'PENDING', ?) RETURNING *
                        """;
                PurchaseOrder po;
                try (PreparedStatement stmt = conn.prepareStatement(insertPO)) {
                    stmt.setObject(1, tenantId);
                    stmt.setObject(2, supplierId);
                    stmt.setBigDecimal(3, total);
                    ResultSet rs = stmt.executeQuery();
                    rs.next();
                    po = PurchaseOrder.fromResultSet(rs);
                }

                String insertItem = """
                        INSERT INTO purchase_items (purchase_order_id, insumo_id, quantity, unit_cost)
                        VALUES (?, ?, ?, ?)
                        """;
                try (PreparedStatement stmt = conn.prepareStatement(insertItem)) {
                    for (int i = 0; i < insumoIds.size(); i++) {
                        stmt.setObject(1, po.id());
                        stmt.setObject(2, insumoIds.get(i));
                        stmt.setBigDecimal(3, quantities.get(i));
                        stmt.setBigDecimal(4, unitCosts.get(i));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                conn.commit();
                return loadPurchaseItems(conn, po);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Erro ao criar ordem de compra: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar ordem de compra.", e);
        }
    }

    /**
     * Completa uma ordem de compra.
     * Motor de Abastecimento: ao marcar como COMPLETED, atualiza estoque e custo médio ponderado.
     */
    public PurchaseOrder completeOrder(UUID orderId, UUID tenantId, UUID userId,
                                       Integer deliveryDays, Integer qualityRating, Integer priceRating) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Buscar ordem com lock
                PurchaseOrder po;
                try (PreparedStatement stmt = conn.prepareStatement(
                        "SELECT po.*, s.name AS supplier_name FROM purchase_orders po " +
                        "JOIN suppliers s ON po.supplier_id = s.id WHERE po.id = ? AND po.tenant_id = ? FOR UPDATE")) {
                    stmt.setObject(1, orderId);
                    stmt.setObject(2, tenantId);
                    ResultSet rs = stmt.executeQuery();
                    if (!rs.next()) throw new RuntimeException("Ordem não encontrada.");
                    po = loadPurchaseItems(conn, PurchaseOrder.fromResultSet(rs));
                }

                if ("COMPLETED".equals(po.status())) {
                    throw new RuntimeException("Esta ordem já foi concluída.");
                }

                // Atualizar status
                try (PreparedStatement stmt = conn.prepareStatement(
                        "UPDATE purchase_orders SET status = 'COMPLETED', delivery_days = ?, quality_rating = ?, price_rating = ?, updated_at = NOW() WHERE id = ?")) {
                    stmt.setObject(1, deliveryDays);
                    stmt.setObject(2, qualityRating);
                    stmt.setObject(3, priceRating);
                    stmt.setObject(4, orderId);
                    stmt.executeUpdate();
                }

                // Motor de Abastecimento: entrada de estoque + custo médio ponderado
                for (PurchaseItem item : po.items()) {
                    // Buscar estoque atual e custo atual
                    BigDecimal currentStock, currentCost;
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "SELECT current_stock, unit_cost FROM insumos WHERE id = ? FOR UPDATE")) {
                        stmt.setObject(1, item.insumoId());
                        ResultSet rs = stmt.executeQuery();
                        rs.next();
                        currentStock = rs.getBigDecimal("current_stock");
                        currentCost = rs.getBigDecimal("unit_cost");
                    }

                    // Custo Médio Ponderado Móvel
                    BigDecimal newStock = currentStock.add(item.quantity());
                    BigDecimal newCost = currentStock.compareTo(BigDecimal.ZERO) == 0
                            ? item.unitCost()
                            : (currentStock.multiply(currentCost).add(item.quantity().multiply(item.unitCost())))
                              .divide(newStock, 4, java.math.RoundingMode.HALF_UP);

                    // Atualizar insumo
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "UPDATE insumos SET current_stock = ?, unit_cost = ?, updated_at = NOW() WHERE id = ?")) {
                        stmt.setBigDecimal(1, newStock);
                        stmt.setBigDecimal(2, newCost);
                        stmt.setObject(3, item.insumoId());
                        stmt.executeUpdate();
                    }

                    // Registrar movimentação INPUT
                    try (PreparedStatement stmt = conn.prepareStatement(
                            "INSERT INTO stock_movements (tenant_id, insumo_id, quantity, type, reason, user_id) VALUES (?, ?, ?, 'INPUT', ?, ?)")) {
                        stmt.setObject(1, tenantId);
                        stmt.setObject(2, item.insumoId());
                        stmt.setBigDecimal(3, item.quantity());
                        stmt.setString(4, "Compra #" + orderId.toString().substring(0, 8));
                        stmt.setObject(5, userId);
                        stmt.executeUpdate();
                    }
                }

                conn.commit();
                log.info("Compra #{} concluída — estoque atualizado.", orderId.toString().substring(0, 8));
                return loadPurchaseItems(conn, po);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Erro ao concluir compra: {}", e.getMessage());
            throw new RuntimeException("Erro ao concluir compra: " + e.getMessage(), e);
        }
    }

    public void cancelOrder(UUID orderId) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE purchase_orders SET status = 'CANCELLED', updated_at = NOW() WHERE id = ? AND status = 'PENDING'")) {
            stmt.setObject(1, orderId);
            int rows = stmt.executeUpdate();
            if (rows == 0) throw new RuntimeException("Não foi possível cancelar (apenas PENDING pode ser cancelado).");
        } catch (SQLException e) {
            log.error("Erro ao cancelar compra: {}", e.getMessage());
            throw new RuntimeException("Erro ao cancelar compra.", e);
        }
    }
}
