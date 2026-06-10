package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.Insumo;
import br.ufpb.eq08.gestor.domain.StockMovement;
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
 * Repositório JDBC para Insumos e Movimentações de Estoque.
 */
public class InsumoRepository {

    private static final Logger log = LoggerFactory.getLogger(InsumoRepository.class);
    private final DataSource ds;

    public InsumoRepository(DataSource ds) {
        this.ds = ds;
    }

    public List<Insumo> findByTenant(UUID tenantId) {
        String sql = """
                SELECT i.*, c.name AS category_name
                FROM insumos i
                LEFT JOIN categories c ON i.category_id = c.id
                WHERE i.tenant_id = ?
                ORDER BY i.name
                """;
        List<Insumo> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(Insumo.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar insumos: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Insumo> findById(UUID id) {
        String sql = """
                SELECT i.*, c.name AS category_name
                FROM insumos i
                LEFT JOIN categories c ON i.category_id = c.id
                WHERE i.id = ?
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(Insumo.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar insumo: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Insumo create(UUID tenantId, UUID categoryId, String name, String unit,
                         BigDecimal currentStock, BigDecimal minimumStock, BigDecimal unitCost) {
        String sql = """
                INSERT INTO insumos (tenant_id, category_id, name, unit, current_stock, minimum_stock, unit_cost)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, categoryId);
            stmt.setString(3, name);
            stmt.setString(4, unit);
            stmt.setBigDecimal(5, currentStock);
            stmt.setBigDecimal(6, minimumStock);
            stmt.setBigDecimal(7, unitCost);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Insumo.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao criar insumo: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar insumo.", e);
        }
        throw new RuntimeException("Erro inesperado ao criar insumo.");
    }

    public Insumo update(UUID id, UUID categoryId, String name, String unit,
                         BigDecimal minimumStock) {
        String sql = """
                UPDATE insumos
                SET category_id = COALESCE(?, category_id),
                    name = COALESCE(?, name),
                    unit = COALESCE(?, unit),
                    minimum_stock = COALESCE(?, minimum_stock),
                    updated_at = NOW()
                WHERE id = ?
                RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, categoryId);
            stmt.setString(2, name);
            stmt.setString(3, unit);
            stmt.setBigDecimal(4, minimumStock);
            stmt.setObject(5, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Insumo.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao atualizar insumo: {}", e.getMessage());
            throw new RuntimeException("Erro ao atualizar insumo.", e);
        }
        throw new RuntimeException("Insumo não encontrado.");
    }

    public void delete(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM insumos WHERE id = ?")) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao excluir insumo: {}", e.getMessage());
            throw new RuntimeException("Erro ao excluir insumo.", e);
        }
    }

    /**
     * Atualiza o estoque atual e o custo médio ponderado de um insumo.
     * Deve ser chamado dentro de uma transação.
     */
    public void updateStockAndCost(Connection conn, UUID insumoId,
                                   BigDecimal newStock, BigDecimal newUnitCost) throws SQLException {
        String sql = """
                UPDATE insumos
                SET current_stock = ?, unit_cost = ?, updated_at = NOW()
                WHERE id = ?
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setBigDecimal(1, newStock);
            stmt.setBigDecimal(2, newUnitCost);
            stmt.setObject(3, insumoId);
            stmt.executeUpdate();
        }
    }

    /** Registra uma movimentação de estoque dentro de uma transação existente. */
    public void insertMovement(Connection conn, UUID tenantId, UUID insumoId,
                               BigDecimal quantity, String type, String reason,
                               UUID userId, UUID orderId) throws SQLException {
        String sql = """
                INSERT INTO stock_movements (tenant_id, insumo_id, quantity, type, reason, user_id, order_id)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, insumoId);
            stmt.setBigDecimal(3, quantity);
            stmt.setString(4, type);
            stmt.setString(5, reason);
            stmt.setObject(6, userId);
            stmt.setObject(7, orderId);
            stmt.executeUpdate();
        }
    }

    public List<StockMovement> findMovementsByTenant(UUID tenantId, int limit) {
        String sql = """
                SELECT sm.*, i.name AS insumo_name, u.name AS user_name
                FROM stock_movements sm
                JOIN insumos i ON sm.insumo_id = i.id
                LEFT JOIN users u ON sm.user_id = u.id
                WHERE sm.tenant_id = ?
                ORDER BY sm.created_at DESC
                LIMIT ?
                """;
        List<StockMovement> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setInt(2, limit);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(StockMovement.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar movimentações: {}", e.getMessage());
        }
        return list;
    }

    /** Busca insumos abaixo do estoque mínimo para alertas de IA. */
    public List<Insumo> findBelowMinimum(UUID tenantId) {
        String sql = """
                SELECT i.*, c.name AS category_name
                FROM insumos i
                LEFT JOIN categories c ON i.category_id = c.id
                WHERE i.tenant_id = ? AND i.current_stock < i.minimum_stock
                ORDER BY (i.current_stock / NULLIF(i.minimum_stock, 0)) ASC
                """;
        List<Insumo> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(Insumo.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar insumos abaixo do mínimo: {}", e.getMessage());
        }
        return list;
    }
}
