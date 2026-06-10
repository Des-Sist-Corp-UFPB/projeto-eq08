package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.AiService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class AiController {
    private final AiService svc;
    public AiController(AiService svc) { this.svc = svc; }

    public void showAnalytics(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Analytics e IA");
        m.put("currentUser", u);
        m.put("recommendations", svc.getAllRecommendations(u.tenantId()));
        m.put("forecasts", svc.getForecasts(u.tenantId()));
        ctx.render("templates/analytics.html", m);
    }

    public void generateForecasts(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        svc.generateForecasts(u.tenantId());
        ctx.redirect("/analytics");
    }

    public void applyRecommendation(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPERVISOR", "SUPER_ADMIN");
        svc.applyRecommendation(UUID.fromString(ctx.pathParam("id")), u.tenantId());
        ctx.redirect("/analytics");
    }

    public void dismissRecommendation(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPERVISOR", "SUPER_ADMIN");
        svc.dismissRecommendation(UUID.fromString(ctx.pathParam("id")), u.tenantId());
        ctx.redirect("/analytics");
    }
}
