package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.SupplierService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class SupplierController {
    private final SupplierService svc;
    public SupplierController(SupplierService svc) { this.svc = svc; }

    public void list(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Fornecedores");
        m.put("currentUser", u);
        m.put("suppliers", svc.list(u.tenantId()));
        ctx.render("templates/fornecedores.html", m);
    }

    public void create(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.create(u.tenantId(), u.id(), ctx.formParam("name"), ctx.formParam("document"),
                ctx.formParam("phone"), ctx.formParam("email"), ctx.formParam("contactName"));
        ctx.redirect("/fornecedores");
    }

    public void update(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        UUID id = UUID.fromString(ctx.pathParam("id"));
        svc.update(id, u.tenantId(), u.id(), ctx.formParam("name"), ctx.formParam("document"),
                ctx.formParam("phone"), ctx.formParam("email"), ctx.formParam("contactName"));
        ctx.redirect("/fornecedores");
    }

    public void delete(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.delete(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/fornecedores");
    }

    public void performance(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        UUID id = UUID.fromString(ctx.pathParam("id"));
        var perf = svc.getPerformance(id, u.tenantId());
        Map<String, Object> m = Map.of("pageTitle", "Performance do Fornecedor", "currentUser", u, "performance", perf,
                "supplier", svc.getById(id, u.tenantId()));
        ctx.render("templates/fornecedor-performance.html", m);
    }
}
