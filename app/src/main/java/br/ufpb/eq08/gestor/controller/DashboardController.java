package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.repository.UserRepository;
import br.ufpb.eq08.gestor.service.AiService;
import br.ufpb.eq08.gestor.service.InsumoService;
import br.ufpb.eq08.gestor.service.OrderService;
import io.javalin.http.Context;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Controller do dashboard com métricas resumidas. */
public class DashboardController {

    private final InsumoService insumoService;
    private final OrderService orderService;
    private final UserRepository userRepo;
    private final AiService aiService;

    public DashboardController(InsumoService insumoService, OrderService orderService,
                               UserRepository userRepo, AiService aiService) {
        this.insumoService = insumoService;
        this.orderService  = orderService;
        this.userRepo      = userRepo;
        this.aiService     = aiService;
    }

    public void show(Context ctx) {
        User currentUser = AuthMiddleware.currentUser(ctx);
        UUID tenantId = currentUser.tenantId();

        // Gerar recomendações de estoque automaticamente a cada acesso ao dashboard
        try { aiService.generateStockRecommendations(tenantId); } catch (Exception ignored) {}

        Map<String, Object> model = new HashMap<>();
        model.put("pageTitle", "Dashboard");
        model.put("currentUser", currentUser);
        model.put("totalInsumos", insumoService.list(tenantId).size());
        model.put("insumosAbaixoMinimo", insumoService.listBelowMinimum(tenantId).size());
        model.put("totalVendas", orderService.countOrders(tenantId));
        model.put("faturamentoRecente", orderService.recentRevenue(tenantId));
        model.put("totalColaboradores", userRepo.findByTenant(tenantId).size());
        model.put("recomendacoesAltas", aiService.countHighImpactRecommendations(tenantId));
        model.put("recomendacoesPendentes", aiService.getPendingRecommendations(tenantId));
        model.put("previsoes", aiService.getForecasts(tenantId));

        ctx.render("templates/dashboard.html", model);
    }
}
