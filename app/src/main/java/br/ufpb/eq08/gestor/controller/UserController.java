package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.UserService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Controller de Usuários / Colaboradores.
 */
public class UserController {

    private final UserService svc;

    public UserController(UserService svc) {
        this.svc = svc;
    }

    public void list(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Colaboradores");
        m.put("currentUser", u);
        m.put("users", svc.list(u.tenantId()));
        ctx.render("templates/usuarios.html", m);
    }

    public void create(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.create(u.tenantId(), u.id(), u.role(),
                ctx.formParam("name"), ctx.formParam("email"),
                ctx.formParam("password"), ctx.formParam("role"));
        ctx.redirect("/usuarios");
    }

    public void update(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        UUID id = UUID.fromString(ctx.pathParam("id"));
        String isActive = ctx.formParam("isActive");
        svc.update(id, u.tenantId(), u.id(), u.role(),
                ctx.formParam("name"), ctx.formParam("email"),
                ctx.formParam("role"),
                isActive != null ? Boolean.parseBoolean(isActive) : null,
                ctx.formParam("password"));
        ctx.redirect("/usuarios");
    }

    public void delete(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.delete(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id(), u.role());
        ctx.redirect("/usuarios");
    }
}
