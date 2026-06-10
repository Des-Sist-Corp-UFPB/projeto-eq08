package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.Category;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JDBC para Category.
 */
public class CategoryRepository {

    private static final Logger log = LoggerFactory.getLogger(CategoryRepository.class);
    private final DataSource ds;

    public CategoryRepository(DataSource ds) {
        this.ds = ds;
    }

    public List<Category> findByTenant(UUID tenantId, String type) {
        String sql = type != null
                ? "SELECT * FROM categories WHERE tenant_id = ? AND type = ? ORDER BY name"
                : "SELECT * FROM categories WHERE tenant_id = ? ORDER BY name";
        List<Category> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            if (type != null) stmt.setString(2, type);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(Category.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar categorias: {}", e.getMessage());
        }
        return list;
    }

    public Optional<Category> findById(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM categories WHERE id = ?")) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(Category.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar categoria: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Category create(UUID tenantId, String name, String type) {
        String sql = "INSERT INTO categories (tenant_id, name, type) VALUES (?, ?, ?) RETURNING *";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setString(2, name);
            stmt.setString(3, type);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Category.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao criar categoria: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar categoria.", e);
        }
        throw new RuntimeException("Erro inesperado.");
    }

    public void delete(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("DELETE FROM categories WHERE id = ?")) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao excluir categoria: {}", e.getMessage());
            throw new RuntimeException("Erro ao excluir categoria.", e);
        }
    }
}
