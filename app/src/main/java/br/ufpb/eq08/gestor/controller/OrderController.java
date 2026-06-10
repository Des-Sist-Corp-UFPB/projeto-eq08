package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.Order;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.service.OrderService;
import br.ufpb.eq08.gestor.service.ProductService;
import io.javalin.http.Context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class OrderController {
    private final OrderService svc;
    private final ProductService productSvc;

    public OrderController(OrderService svc, ProductService productSvc) {
        this.svc = svc; this.productSvc = productSvc;
    }

    public void showPdv(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = new HashMap<>();
        m.put("pageTitle", "PDV — Ponto de Venda");
        m.put("currentUser", u);
        m.put("products", productSvc.list(u.tenantId()));
        m.put("orders", svc.listOrders(u.tenantId(), 10));
        ctx.render("templates/pdv.html", m);
    }

    public void createOrder(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        List<String> productIds = ctx.formParams("productId");
        List<String> quantities = ctx.formParams("quantity");
        List<UUID> ids = new ArrayList<>();
        List<Integer> qtys = new ArrayList<>();
        for (int i = 0; i < productIds.size(); i++) {
            if (!productIds.get(i).isBlank()) {
                ids.add(UUID.fromString(productIds.get(i)));
                String q = quantities.get(i);
                qtys.add(Integer.parseInt((q == null || q.isBlank()) ? "1" : q));
            }
        }
        Order order = svc.createOrder(u.tenantId(), u.id(), ids, qtys);

        if (ctx.header("HX-Request") != null) {
            ctx.html("<div class=\"alert alert-success\"><i class=\"bi bi-check-circle me-2\"></i>" +
                     "Venda registrada! Total: R$ " + order.totalPrice() + "</div>");
        } else {
            ctx.redirect("/pdv");
        }
    }

    public void listOrders(Context ctx) {
        User u = AuthMiddleware.currentUser(ctx);
        Map<String, Object> m = Map.of("currentUser", u, "orders", svc.listOrders(u.tenantId(), 50));
        ctx.render("templates/fragments/orders-table.html", m);
    }
}
