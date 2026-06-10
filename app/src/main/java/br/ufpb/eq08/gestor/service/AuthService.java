package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.auth.JwtUtil;
import br.ufpb.eq08.gestor.auth.PasswordUtil;
import br.ufpb.eq08.gestor.domain.Tenant;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import br.ufpb.eq08.gestor.repository.TenantRepository;
import br.ufpb.eq08.gestor.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Map;

/**
 * Serviço de autenticação: registro de tenant, login, logout.
 */
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private final TenantRepository tenantRepo;
    private final UserRepository userRepo;
    private final AuditRepository auditRepo;

    public AuthService(TenantRepository tenantRepo, UserRepository userRepo, AuditRepository auditRepo) {
        this.tenantRepo = tenantRepo;
        this.userRepo   = userRepo;
        this.auditRepo  = auditRepo;
    }

    /**
     * Onboarding: registra nova empresa e usuário OWNER.
     */
    public User registerTenant(String companyName, String slug,
                               String adminName, String adminEmail, String adminPassword) {
        // Validações
        if (tenantRepo.findBySlug(slug).isPresent()) {
            throw AppException.conflict("Este identificador (slug) já está sendo utilizado por outra empresa.");
        }
        if (userRepo.findByEmail(adminEmail).isPresent()) {
            throw AppException.conflict("O e-mail informado já está cadastrado no sistema.");
        }

        // Criar tenant
        Tenant tenant = tenantRepo.create(companyName, slug);

        // Criar OWNER
        String hashed = PasswordUtil.hash(adminPassword);
        User owner = userRepo.create(tenant.id(), adminName, adminEmail, hashed, "OWNER");

        // Audit log
        auditRepo.log(tenant.id(), owner.id(), "TENANT_REGISTER", "tenants",
                tenant.id().toString(),
                null,
                Map.of("tenant_name", tenant.name(), "owner_email", owner.email()),
                null);

        log.info("Novo tenant registrado: {} ({})", tenant.name(), tenant.slug());
        return owner;
    }

    /**
     * Autentica usuário e gera JWT.
     *
     * @return JWT token string
     */
    public String login(String email, String password, String ipAddress) {
        User user = userRepo.findByEmail(email)
                .orElseThrow(() -> AppException.badRequest("E-mail ou senha incorretos."));

        if (!PasswordUtil.verify(password, user.hashedPassword())) {
            throw AppException.badRequest("E-mail ou senha incorretos.");
        }

        if (!user.isActive()) {
            throw AppException.badRequest("Usuário inativo. Entre em contato com o administrador.");
        }

        // Verificar tenant ativo (se não é SUPER_ADMIN)
        if (user.tenantId() != null) {
            Tenant tenant = tenantRepo.findById(user.tenantId())
                    .orElseThrow(() -> AppException.badRequest("Empresa não encontrada."));
            if (!tenant.isActive()) {
                throw AppException.badRequest("Empresa associada está inativa ou suspensa.");
            }
        }

        // Gerar JWT
        String token = JwtUtil.generateToken(user.id(), user.tenantId(), user.role());

        // Audit log
        if (user.tenantId() != null) {
            auditRepo.log(user.tenantId(), user.id(), "USER_LOGIN", "users",
                    user.id().toString(), null,
                    Map.of("email", user.email(), "role", user.role()),
                    ipAddress);
        }

        log.info("Login: {} [{}]", user.email(), user.role());
        return token;
    }
}
