package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.InsumoService;
import br.ufpb.eq08.gestor.service.PurchaseService;
import br.ufpb.eq08.gestor.service.SupplierService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PurchaseController {
    private final PurchaseService svc;
    private final SupplierService supplierSvc;
    private final InsumoService insumoSvc;

    public PurchaseController(PurchaseService svc, SupplierService supplierSvc, InsumoService insumoSvc) {
        this.svc = svc; this.supplierSvc = supplierSvc; this.insumoSvc = insumoSvc;
    }

    public void list(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Compras");
        m.put("currentUser", u);
        m.put("purchases", svc.list(u.tenantId()));
        m.put("suppliers", supplierSvc.list(u.tenantId()));
        m.put("insumos", insumoSvc.list(u.tenantId()));
        ctx.render("templates/compras.html", m);
    }

    public void create(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        UUID supplierId = UUID.fromString(ctx.formParam("supplierId"));
        List<String> insumoIds = ctx.formParams("insumoId");
        List<String> qtys = ctx.formParams("quantity");
        List<String> costs = ctx.formParams("unitCost");
        List<UUID> ids = new ArrayList<>();
        List<BigDecimal> quantities = new ArrayList<>();
        List<BigDecimal> unitCosts = new ArrayList<>();
        for (int i = 0; i < insumoIds.size(); i++) {
            if (!insumoIds.get(i).isBlank()) {
                ids.add(UUID.fromString(insumoIds.get(i)));
                quantities.add(new BigDecimal(qtys.get(i)));
                unitCosts.add(new BigDecimal(costs.get(i)));
            }
        }
        svc.create(u.tenantId(), u.id(), supplierId, ids, quantities, unitCosts);
        ctx.redirect("/compras");
    }

    public void complete(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        UUID id = UUID.fromString(ctx.pathParam("id"));
        String dd = ctx.formParam("deliveryDays");
        String qr = ctx.formParam("qualityRating");
        String pr = ctx.formParam("priceRating");
        svc.complete(id, u.tenantId(), u.id(),
                dd != null && !dd.isBlank() ? Integer.parseInt(dd) : null,
                qr != null && !qr.isBlank() ? Integer.parseInt(qr) : null,
                pr != null && !pr.isBlank() ? Integer.parseInt(pr) : null);
        ctx.redirect("/compras");
    }

    public void cancel(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.cancel(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/compras");
    }
}
