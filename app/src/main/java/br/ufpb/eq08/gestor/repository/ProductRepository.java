package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.Order;
import br.ufpb.eq08.gestor.domain.OrderItem;
import br.ufpb.eq08.gestor.domain.Product;
import br.ufpb.eq08.gestor.domain.ProductIngredient;
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
 * Repositório JDBC para Products e Orders.
 * Inclui suporte à Ficha Técnica e ao Motor de Baixa Automática de estoque.
 */
public class ProductRepository {

    private static final Logger log = LoggerFactory.getLogger(ProductRepository.class);
    private final DataSource ds;

    public ProductRepository(DataSource ds) {
        this.ds = ds;
    }

    // ========================================================================
    // PRODUCTS
    // ========================================================================

    public List<Product> findByTenant(UUID tenantId) {
        String sql = """
                SELECT p.*, c.name AS category_name
                FROM products p
                LEFT JOIN categories c ON p.category_id = c.id
                WHERE p.tenant_id = ?
                ORDER BY p.name
                """;
        List<Product> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                Product p = Product.fromResultSet(rs);
                list.add(loadIngredients(conn, p));
            }
        } catch (SQLException e) {
            log.error("Erro ao listar produtos: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Product> findById(UUID id) {
        String sql = """
                SELECT p.*, c.name AS category_name
                FROM products p
                LEFT JOIN categories c ON p.category_id = c.id
                WHERE p.id = ?
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Product p = Product.fromResultSet(rs);
                return Optional.of(loadIngredients(conn, p));
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar produto: {}", e.getMessage());
        }
        return Optional.empty();
    }

    private Product loadIngredients(Connection conn, Product p) throws SQLException {
        String sql = """
                SELECT pi.*, i.name AS insumo_name, i.unit AS insumo_unit, i.unit_cost
                FROM product_ingredients pi
                JOIN insumos i ON pi.insumo_id = i.id
                WHERE pi.product_id = ?
                """;
        List<ProductIngredient> ings = new ArrayList<>();
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, p.id());
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) ings.add(ProductIngredient.fromResultSet(rs));
        }
        return new Product(p.id(), p.tenantId(), p.categoryId(), p.categoryName(),
                p.name(), p.price(), p.isActive(), ings, p.createdAt(), p.updatedAt());
    }

    public Product create(UUID tenantId, UUID categoryId, String name, BigDecimal price,
                          List<UUID> ingredientIds, List<BigDecimal> ingredientQtys) {
        try (Connection conn = ds.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // Inserir produto
                String insertProduct = """
                        INSERT INTO products (tenant_id, category_id, name, price, is_active)
                        VALUES (?, ?, ?, ?, true) RETURNING *
                        """;
                Product product;
                try (PreparedStatement stmt = conn.prepareStatement(insertProduct)) {
                    stmt.setObject(1, tenantId);
                    stmt.setObject(2, categoryId);
                    stmt.setString(3, name);
                    stmt.setBigDecimal(4, price);
                    ResultSet rs = stmt.executeQuery();
                    rs.next();
                    product = Product.fromResultSet(rs);
                }

                // Inserir ingredientes da Ficha Técnica
                String insertIng = "INSERT INTO product_ingredients (product_id, insumo_id, quantity) VALUES (?, ?, ?)";
                try (PreparedStatement stmt = conn.prepareStatement(insertIng)) {
                    for (int i = 0; i < ingredientIds.size(); i++) {
                        stmt.setObject(1, product.id());
                        stmt.setObject(2, ingredientIds.get(i));
                        stmt.setBigDecimal(3, ingredientQtys.get(i));
                        stmt.addBatch();
                    }
                    stmt.executeBatch();
                }

                conn.commit();
                log.info("Produto criado: {} com {} ingredientes", product.name(), ingredientIds.size());
                return loadIngredients(conn, product);
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Erro ao criar produto: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar produto.", e);
        }
    }

    public void delete(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM products WHERE id = ?")) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao excluir produto: {}", e.getMessage());
            throw new RuntimeException("Erro ao excluir produto.", e);
        }
    }

    // ========================================================================
    // ORDERS + MOTOR DE BAIXA AUTOMÁTICA
    // ========================================================================

    /**
     * Registra uma venda e executa a baixa automática de estoque.
     * Operação totalmente atômica (rollback em caso de estoque insuficiente).
     */
    public Order createOrder(UUID tenantId, UUID userId,
                             List<UUID> productIds, List<Integer> quantities,
                             DataSource dataSource) {
        try (Connection conn = dataSource.getConnection()) {
            conn.setAutoCommit(false);
            try {
                // 1. Calcular total e validar produtos
                BigDecimal total = BigDecimal.ZERO;
                List<Product> products = new ArrayList<>();
                for (UUID pid : productIds) {
                    String pSql = """
                            SELECT p.*, c.name AS category_name
                            FROM products p LEFT JOIN categories c ON p.category_id = c.id
                            WHERE p.id = ? AND p.tenant_id = ? AND p.is_active = true
                            FOR UPDATE
                            """;
                    try (PreparedStatement stmt = conn.prepareStatement(pSql)) {
                        stmt.setObject(1, pid);
                        stmt.setObject(2, tenantId);
                        ResultSet rs = stmt.executeQuery();
                        if (!rs.next()) {
                            throw new RuntimeException("Produto " + pid + " não encontrado ou inativo.");
                        }
                        products.add(loadIngredients(conn, Product.fromResultSet(rs)));
                    }
                }

                for (int i = 0; i < products.size(); i++) {
                    total = total.add(products.get(i).price().multiply(BigDecimal.valueOf(quantities.get(i))));
                }

                // 2. Criar a venda
                String insertOrder = """
                        INSERT INTO orders (tenant_id, user_id, total_price)
                        VALUES (?, ?, ?) RETURNING *
                        """;
                Order order;
                try (PreparedStatement stmt = conn.prepareStatement(insertOrder)) {
                    stmt.setObject(1, tenantId);
                    stmt.setObject(2, userId);
                    stmt.setBigDecimal(3, total);
                    ResultSet rs = stmt.executeQuery();
                    rs.next();
                    order = Order.fromResultSet(rs);
                }

                // 3. Inserir itens e executar baixa automática de estoque
                for (int i = 0; i < products.size(); i++) {
                    Product p = products.get(i);
                    int qty = quantities.get(i);

                    // Inserir item do pedido
                    String insertItem = """
                            INSERT INTO order_items (order_id, product_id, quantity, unit_price)
                            VALUES (?, ?, ?, ?)
                            """;
                    try (PreparedStatement stmt = conn.prepareStatement(insertItem)) {
                        stmt.setObject(1, order.id());
                        stmt.setObject(2, p.id());
                        stmt.setInt(3, qty);
                        stmt.setBigDecimal(4, p.price());
                        stmt.executeUpdate();
                    }

                    // Motor de Baixa Automática: descontar ingredientes
                    for (ProductIngredient ing : p.ingredients()) {
                        BigDecimal consumed = ing.quantity().multiply(BigDecimal.valueOf(qty));

                        // Verificar estoque atual (com lock FOR UPDATE)
                        BigDecimal currentStock;
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "SELECT current_stock FROM insumos WHERE id = ? FOR UPDATE")) {
                            stmt.setObject(1, ing.insumoId());
                            ResultSet rs = stmt.executeQuery();
                            rs.next();
                            currentStock = rs.getBigDecimal("current_stock");
                        }

                        if (currentStock.compareTo(consumed) < 0) {
                            throw new RuntimeException(
                                    "Estoque insuficiente do insumo '" + ing.insumoName() +
                                    "'. Disponível: " + currentStock + " " + ing.insumoUnit() +
                                    ", necessário: " + consumed + " " + ing.insumoUnit()
                            );
                        }

                        // Abater estoque
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "UPDATE insumos SET current_stock = current_stock - ?, updated_at = NOW() WHERE id = ?")) {
                            stmt.setBigDecimal(1, consumed);
                            stmt.setObject(2, ing.insumoId());
                            stmt.executeUpdate();
                        }

                        // Registrar movimento AUTOMATIC_CONSUMPTION
                        try (PreparedStatement stmt = conn.prepareStatement(
                                "INSERT INTO stock_movements (tenant_id, insumo_id, quantity, type, reason, user_id, order_id) VALUES (?, ?, ?, 'AUTOMATIC_CONSUMPTION', ?, ?, ?)")) {
                            stmt.setObject(1, tenantId);
                            stmt.setObject(2, ing.insumoId());
                            stmt.setBigDecimal(3, consumed.negate());
                            stmt.setString(4, "Venda #" + order.id().toString().substring(0, 8));
                            stmt.setObject(5, userId);
                            stmt.setObject(6, order.id());
                            stmt.executeUpdate();
                        }
                    }
                }

                conn.commit();
                log.info("Venda registrada: #{} — R$ {}", order.id().toString().substring(0, 8), total);
                return order;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            log.error("Erro ao registrar venda: {}", e.getMessage());
            throw new RuntimeException("Erro ao registrar venda.", e);
        }
    }

    public List<Order> findOrdersByTenant(UUID tenantId, int limit) {
        String sql = """
                SELECT o.*, u.name AS user_name
                FROM orders o
                LEFT JOIN users u ON o.user_id = u.id
                WHERE o.tenant_id = ?
                ORDER BY o.created_at DESC
                LIMIT ?
                """;
        List<Order> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(Order.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar vendas: {}", e.getMessage());
        }
        return list;
    }
}
