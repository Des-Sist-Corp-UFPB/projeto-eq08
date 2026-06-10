package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.CategoryService;
import br.ufpb.eq08.gestor.service.InsumoService;
import br.ufpb.eq08.gestor.service.ProductService;
import io.javalin.http.Context;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ProductController {
    private final ProductService svc;
    private final InsumoService insumoSvc;
    private final CategoryService catSvc;

    public ProductController(ProductService svc, InsumoService insumoSvc, CategoryService catSvc) {
        this.svc = svc; this.insumoSvc = insumoSvc; this.catSvc = catSvc;
    }

    public void list(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "Produtos / Cardápio");
        m.put("currentUser", u);
        m.put("products", svc.list(u.tenantId()));
        m.put("insumos", insumoSvc.list(u.tenantId()));
        m.put("categories", catSvc.list(u.tenantId(), "PRODUCT"));
        ctx.render("templates/produtos.html", m);
    }

    public void create(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        String catId = ctx.formParam("categoryId");

        List<String> ingIds  = ctx.formParams("ingredientId");
        List<String> ingQtys = ctx.formParams("ingredientQty");
        List<UUID> ids = new ArrayList<>();
        List<BigDecimal> qtys = new ArrayList<>();
        for (int i = 0; i < ingIds.size(); i++) {
            if (!ingIds.get(i).isBlank() && !ingQtys.get(i).isBlank()) {
                ids.add(UUID.fromString(ingIds.get(i)));
                qtys.add(new BigDecimal(ingQtys.get(i)));
            }
        }

        String priceStr = ctx.formParam("price");
        BigDecimal price = new BigDecimal((priceStr == null || priceStr.isBlank()) ? "0" : priceStr);

        svc.create(u.tenantId(), u.id(),
                catId != null && !catId.isBlank() ? UUID.fromString(catId) : null,
                ctx.formParam("name"), price, ids, qtys);
        ctx.redirect("/produtos");
    }

    public void update(Context ctx) {
        ctx.redirect("/produtos");
    }

    public void delete(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER", "SUPER_ADMIN");
        svc.delete(UUID.fromString(ctx.pathParam("id")), u.tenantId(), u.id());
        ctx.redirect("/produtos");
    }
}
