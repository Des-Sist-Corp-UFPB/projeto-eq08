package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.CategoryService;
import br.ufpb.eq08.gestor.service.InsumoService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class InsumoController {
    private final InsumoService svc;
    private final CategoryService catSvc;

    public InsumoController(InsumoService svc, CategoryService catSvc) {
        this.svc = svc; this.catSvc = catSvc;
    }

    public void list(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Insumos / Estoque");
        m.put("currentUser", u);
        m.put("insumos", svc.list(u.tenantId()));
        m.put("categories", catSvc.list(u.tenantId(), "INSUMO"));
        m.put("movements", svc.listMovements(u.tenantId(), 20));
        ctx.render("templates/insumos.html", m);
    }

    public void create(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        String catId = ctx.formParam("categoryId");
        svc.create(u.tenantId(), u.id(),
                catId != null && !catId.isBlank() ? UUID.fromString(catId) : null,
                ctx.formParam("name"), ctx.formParam("unit"),
                new BigDecimal(def(ctx.formParam("currentStock"), "0")),
                new BigDecimal(def(ctx.formParam("minimumStock"), "0")),
                new BigDecimal(def(ctx.formParam("unitCost"), "0")));
        ctx.redirect("/insumos");
    }

    public void update(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        String catId = ctx.formParam("categoryId");
        String ms = ctx.formParam("minimumStock");
        svc.update(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id(),
                catId != null && !catId.isBlank() ? UUID.fromString(catId) : null,
                ctx.formParam("name"), ctx.formParam("unit"),
                ms != null && !ms.isBlank() ? new BigDecimal(ms) : null);
        ctx.redirect("/insumos");
    }

    public void delete(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.delete(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/insumos");
    }

    public void addMovement(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        svc.addMovement(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id(),
                new BigDecimal(ctx.formParam("quantity")),
                ctx.formParam("type"), ctx.formParam("reason"));
        ctx.redirect("/insumos");
    }

    public void listMovements(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = Map.of("currentUser", u, "movements", svc.listMovements(u.tenantId(), 50));
        ctx.render("templates/fragments/movements-table.html", m);
    }

    private String def(String s, String d) { return (s == null || s.isBlank()) ? d : s; }
}
