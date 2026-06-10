package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.Tenant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JDBC para operações de Tenant.
 */
public class TenantRepository {

    private static final Logger log = LoggerFactory.getLogger(TenantRepository.class);
    private final DataSource ds;

    public TenantRepository(DataSource ds) {
        this.ds = ds;
    }

    public Optional<Tenant> findById(UUID id) {
        String sql = "SELECT * FROM tenants WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(Tenant.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar tenant por ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<Tenant> findBySlug(String slug) {
        String sql = "SELECT * FROM tenants WHERE slug = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, slug);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(Tenant.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar tenant por slug: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Tenant create(String name, String slug) {
        String sql = """
                INSERT INTO tenants (name, slug, status)
                VALUES (?, ?, 'active')
                RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, name);
            stmt.setString(2, slug);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                Tenant t = Tenant.fromResultSet(rs);
                log.info("Tenant criado: {} ({})", t.name(), t.id());
                return t;
            }
        } catch (SQLException e) {
            log.error("Erro ao criar tenant: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar empresa.", e);
        }
        throw new RuntimeException("Erro inesperado ao criar empresa.");
    }
}
