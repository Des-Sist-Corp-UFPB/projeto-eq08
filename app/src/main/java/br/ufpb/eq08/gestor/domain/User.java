package br.ufpb.eq08.gestor.domain;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Representa um usuário/colaborador do sistema.
 * Imutável — usa Java Record.
 *
 * Roles: SUPER_ADMIN | OWNER | MANAGER | SUPERVISOR | OPERATOR
 */
public record User(
        UUID id,
        UUID tenantId,
        String name,
        String email,
        String hashedPassword,
        String role,
        boolean isActive,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static User fromResultSet(ResultSet rs) throws SQLException {
        String tenantIdStr = rs.getString("tenant_id");
        return new User(
                rs.getObject("id", UUID.class),
                tenantIdStr != null ? UUID.fromString(tenantIdStr) : null,
                rs.getString("name"),
                rs.getString("email"),
                rs.getString("hashed_password"),
                rs.getString("role"),
                rs.getBoolean("is_active"),
                rs.getObject("created_at", OffsetDateTime.class),
                rs.getObject("updated_at", OffsetDateTime.class)
        );
    }

    /** Hierarquia de roles para comparação de permissões */
    public int roleLevel() {
        return switch (role) {
            case "SUPER_ADMIN" -> 5;
            case "OWNER"       -> 4;
            case "MANAGER"     -> 3;
            case "SUPERVISOR"  -> 2;
            case "OPERATOR"    -> 1;
            default            -> 0;
        };
    }

    public boolean hasRole(String... roles) {
        for (String r : roles) {
            if (role.equals(r)) return true;
        }
        return false;
    }

    public boolean canManage() {
        return hasRole("OWNER", "MANAGER", "SUPER_ADMIN");
    }

    /** Versão segura sem senha para exibição em templates */
    public User withoutPassword() {
        return new User(id, tenantId, name, email, null, role, isActive, createdAt, updatedAt);
    }
}
