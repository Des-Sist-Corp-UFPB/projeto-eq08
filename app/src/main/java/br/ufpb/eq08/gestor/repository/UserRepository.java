package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório JDBC para operações de User.
 */
public class UserRepository {

    private static final Logger log = LoggerFactory.getLogger(UserRepository.class);
    private final DataSource ds;

    public UserRepository(DataSource ds) {
        this.ds = ds;
    }

    public Optional<User> findById(UUID id) {
        String sql = "SELECT * FROM users WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(User.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar usuário por ID: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public Optional<User> findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(User.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar usuário por email: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public List<User> findByTenant(UUID tenantId) {
        String sql = "SELECT * FROM users WHERE tenant_id = ? ORDER BY name";
        List<User> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(User.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar usuários: {}", e.getMessage());
        }
        return list;
    }

    public User create(UUID tenantId, String name, String email, String hashedPassword, String role) {
        String sql = """
                INSERT INTO users (tenant_id, name, email, hashed_password, role, is_active)
                VALUES (?, ?, ?, ?, ?, true)
                RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setString(2, name);
            stmt.setString(3, email);
            stmt.setString(4, hashedPassword);
            stmt.setString(5, role);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                User u = User.fromResultSet(rs);
                log.info("Usuário criado: {} [{}]", u.email(), u.role());
                return u;
            }
        } catch (SQLException e) {
            log.error("Erro ao criar usuário: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar usuário.", e);
        }
        throw new RuntimeException("Erro inesperado ao criar usuário.");
    }

    public User update(UUID id, String name, String email, String role, Boolean isActive, String hashedPassword) {
        StringBuilder sql = new StringBuilder("UPDATE users SET updated_at = NOW()");
        List<Object> params = new ArrayList<>();

        if (name != null)           { sql.append(", name = ?");            params.add(name); }
        if (email != null)          { sql.append(", email = ?");           params.add(email); }
        if (role != null)           { sql.append(", role = ?");            params.add(role); }
        if (isActive != null)       { sql.append(", is_active = ?");       params.add(isActive); }
        if (hashedPassword != null) { sql.append(", hashed_password = ?"); params.add(hashedPassword); }

        sql.append(" WHERE id = ? RETURNING *");
        params.add(id);

        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return User.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao atualizar usuário: {}", e.getMessage());
            throw new RuntimeException("Erro ao atualizar usuário.", e);
        }
        throw new RuntimeException("Usuário não encontrado.");
    }

    public void delete(UUID id) {
        String sql = "DELETE FROM users WHERE id = ?";
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, id);
            stmt.executeUpdate();
            log.info("Usuário {} removido.", id);
        } catch (SQLException e) {
            log.error("Erro ao excluir usuário: {}", e.getMessage());
            throw new RuntimeException("Erro ao excluir usuário.", e);
        }
    }
}
