package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.CategoryService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CategoryController {
    private final CategoryService svc;
    public CategoryController(CategoryService svc) { this.svc = svc; }

    public void list(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Categorias");
        m.put("currentUser", u);
        m.put("categories", svc.list(u.tenantId(), ctx.queryParam("type")));
        ctx.render("templates/categorias.html", m);
    }

    public void create(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.create(u.tenantId(), u.id(), ctx.formParam("name"), ctx.formParam("type"));
        ctx.redirect("/categorias");
    }

    public void delete(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.delete(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/categorias");
    }
}
