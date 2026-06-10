package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.repository.AuditRepository;
import io.javalin.http.Context;

import java.util.Map;

public class AuditController {
    private final AuditRepository repo;
    public AuditController(AuditRepository repo) { this.repo = repo; }

    public void list(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "SUPER_ADMIN");
        Map<String, Object> m = Map.of("pageTitle", "Logs de Auditoria", "currentUser", u,
                "logs", repo.findByTenant(u.tenantId(), 100));
        ctx.render("templates/auditoria.html", m);
    }
}
