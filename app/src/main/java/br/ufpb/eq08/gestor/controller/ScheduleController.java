package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.ScheduleService;
import br.ufpb.eq08.gestor.service.UserService;
import io.javalin.http.Context;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ScheduleController {
    private final ScheduleService svc;
    private final UserService userSvc;

    public ScheduleController(ScheduleService svc, UserService userSvc) {
        this.svc = svc; this.userSvc = userSvc;
    }

    public void listSchedules(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Escalas de Trabalho");
        m.put("currentUser", u);
        m.put("schedules", svc.listSchedules(u.tenantId(), null, null));
        m.put("users", userSvc.list(u.tenantId()));
        ctx.render("templates/escalas.html", m);
    }

    public void createSchedule(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPERVISOR", "SUPER_ADMIN");
        UUID operatorId = UUID.fromString(ctx.formParam("userId"));
        svc.createSchedule(u.tenantId(), u.id(), operatorId,
                LocalDate.parse(ctx.formParam("shiftDate")),
                ctx.formParam("startTime"), ctx.formParam("endTime"), ctx.formParam("notes"));
        ctx.redirect("/escalas");
    }

    public void deleteSchedule(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPERVISOR", "SUPER_ADMIN");
        svc.deleteSchedule(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/escalas");
    }

    public void listTrades(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = Map.of("pageTitle", "Trocas de Turno", "currentUser", u,
                "trades", svc.listTrades(u.tenantId()));
        ctx.render("templates/trocas.html", m);
    }

    public void createTrade(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        svc.createTrade(u.tenantId(), u.id(),
                UUID.fromString(ctx.formParam("targetUserId")),
                UUID.fromString(ctx.formParam("requestingScheduleId")),
                UUID.fromString(ctx.formParam("targetScheduleId")));
        ctx.redirect("/escalas/trocas");
    }

    public void approveTrade(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPERVISOR", "SUPER_ADMIN");
        svc.approveTrade(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/escalas/trocas");
    }

    public void rejectTrade(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPERVISOR", "SUPER_ADMIN");
        svc.rejectTrade(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/escalas/trocas");
    }

    public void listAbsences(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = Map.of("pageTitle", "Afastamentos / Férias", "currentUser", u,
                "absences", svc.listAbsences(u.tenantId()),
                "users", userSvc.list(u.tenantId()));
        ctx.render("templates/afastamentos.html", m);
    }

    public void createAbsence(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        UUID userId = UUID.fromString(ctx.formParam("userId"));
        svc.createAbsence(u.tenantId(), userId, u.id(), u.role(),
                LocalDate.parse(ctx.formParam("startDate")),
                LocalDate.parse(ctx.formParam("endDate")),
                ctx.formParam("type"), ctx.formParam("reason"));
        ctx.redirect("/escalas/afastamentos");
    }

    public void approveAbsence(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPERVISOR", "SUPER_ADMIN");
        svc.approveAbsence(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/escalas/afastamentos");
    }
}
