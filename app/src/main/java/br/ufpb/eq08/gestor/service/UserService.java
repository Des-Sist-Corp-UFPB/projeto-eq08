package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.auth.PasswordUtil;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Serviço de Usuários com CRUD e validação RBAC. */
public class UserService {

    private final UserRepository userRepo;
    private final AuditRepository auditRepo;

    public UserService(UserRepository userRepo, AuditRepository auditRepo) {
        this.userRepo  = userRepo;
        this.auditRepo = auditRepo;
    }

    public List<User> list(UUID tenantId) {
        return userRepo.findByTenant(tenantId);
    }

    public User getById(UUID id, UUID tenantId) {
        return userRepo.findById(id)
                .filter(u -> u.tenantId() != null && u.tenantId().equals(tenantId))
                .orElseThrow(() -> AppException.notFound("Usuário não encontrado."));
    }

    public User create(UUID tenantId, UUID currentUserId, String currentRole,
                       String name, String email, String password, String role) {
        // Validar RBAC: MANAGER não pode criar OWNER/SUPER_ADMIN
        if ("MANAGER".equals(currentRole) && (role.equals("OWNER") || role.equals("SUPER_ADMIN"))) {
            throw AppException.forbidden("Você não tem permissão para cadastrar usuários com este perfil.");
        }

        if (userRepo.findByEmail(email).isPresent()) {
            throw AppException.conflict("O e-mail informado já está cadastrado.");
        }

        User created = userRepo.create(tenantId, name, email, PasswordUtil.hash(password), role);
        auditRepo.log(tenantId, currentUserId, "USER_CREATE", "users", created.id().toString(),
                null, Map.of("email", email, "role", role), null);
        return created;
    }

    public User update(UUID id, UUID tenantId, UUID currentUserId, String currentRole,
                       String name, String email, String role, Boolean isActive, String password) {
        User existing = getById(id, tenantId);

        // MANAGER não pode promover para OWNER/SUPER_ADMIN
        if (role != null && "MANAGER".equals(currentRole) && (role.equals("OWNER") || role.equals("SUPER_ADMIN"))) {
            throw AppException.forbidden("Sem permissão para atribuir este perfil.");
        }

        // Verificar unicidade de email
        if (email != null && !email.equals(existing.email()) && userRepo.findByEmail(email).isPresent()) {
            throw AppException.conflict("O e-mail informado já está cadastrado.");
        }

        String hashed = (password != null && !password.isBlank()) ? PasswordUtil.hash(password) : null;

        Map<String, Object> before = Map.of("name", existing.name(), "role", existing.role(), "is_active", existing.isActive());
        User updated = userRepo.update(id, name, email, role, isActive, hashed);
        auditRepo.log(tenantId, currentUserId, "USER_UPDATE", "users", id.toString(),
                before, Map.of("name", updated.name(), "role", updated.role(), "is_active", updated.isActive()), null);
        return updated;
    }

    public void delete(UUID id, UUID tenantId, UUID currentUserId, String currentRole) {
        if (id.equals(currentUserId)) {
            throw AppException.badRequest("Você não pode excluir sua própria conta.");
        }
        User target = getById(id, tenantId);
        if ("MANAGER".equals(currentRole) && (target.role().equals("OWNER") || target.role().equals("SUPER_ADMIN"))) {
            throw AppException.forbidden("Nível de permissão insuficiente.");
        }
        auditRepo.log(tenantId, currentUserId, "USER_DELETE", "users", id.toString(),
                Map.of("email", target.email(), "role", target.role()), null, null);
        userRepo.delete(id);
    }
}
