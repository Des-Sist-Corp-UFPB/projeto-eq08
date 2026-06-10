package br.ufpb.eq08.gestor;

import br.ufpb.eq08.gestor.auth.AuthMiddleware;
import br.ufpb.eq08.gestor.config.AppConfig;
import br.ufpb.eq08.gestor.config.DatabaseConfig;
import br.ufpb.eq08.gestor.controller.*;
import br.ufpb.eq08.gestor.exception.AppException;
import br.ufpb.eq08.gestor.repository.*;
import br.ufpb.eq08.gestor.service.*;
import io.javalin.Javalin;
import io.javalin.http.staticfiles.Location;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.util.Map;

/**
 * Ponto de entrada da aplicação Gestor de Negócio SaaS.
 *
 * Ordem de inicialização:
 * 1. Banco de dados + Flyway
 * 2. Repositórios (JDBC)
 * 3. Serviços (lógica de negócio)
 * 4. Controllers (handlers Javalin)
 * 5. Javalin com middleware, rotas e tratamento de erros
 */
public class Main {

    private static final Logger log = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        log.info("=== Iniciando Gestor de Negócio SaaS ===");
        log.info("Ambiente: {}", AppConfig.APP_ENV);

        // 1. Banco de dados
        DataSource ds = DatabaseConfig.init();

        // 2. Repositórios
        TenantRepository tenantRepo   = new TenantRepository(ds);
        UserRepository   userRepo     = new UserRepository(ds);
        AuditRepository  auditRepo    = new AuditRepository(ds);
        CategoryRepository catRepo    = new CategoryRepository(ds);
        InsumoRepository insumoRepo   = new InsumoRepository(ds);
        ProductRepository productRepo = new ProductRepository(ds);
        SupplierRepository supplierRepo = new SupplierRepository(ds);
        ScheduleRepository scheduleRepo = new ScheduleRepository(ds);
        AiRepository aiRepo           = new AiRepository(ds);

        // 3. Serviços
        AuthService    authService    = new AuthService(tenantRepo, userRepo, auditRepo);
        UserService    userService    = new UserService(userRepo, auditRepo);
        CategoryService catService    = new CategoryService(catRepo, auditRepo);
        InsumoService  insumoService  = new InsumoService(insumoRepo, auditRepo, ds);
        ProductService productService = new ProductService(productRepo, insumoRepo, catRepo, auditRepo, ds);
        OrderService   orderService   = new OrderService(productRepo, auditRepo, ds);
        SupplierService supplierService = new SupplierService(supplierRepo, auditRepo);
        PurchaseService purchaseService = new PurchaseService(supplierRepo, auditRepo);
        ScheduleService scheduleService = new ScheduleService(scheduleRepo, auditRepo);
        AiService      aiService       = new AiService(aiRepo, insumoRepo, supplierRepo);

        // 4. Controllers
        AuthController     authController     = new AuthController(authService);
        DashboardController dashboardCtrl     = new DashboardController(insumoService, orderService, userRepo, aiService);
        UserController     userController     = new UserController(userService);
        CategoryController catController      = new CategoryController(catService);
        InsumoController   insumoController   = new InsumoController(insumoService, catService);
        ProductController  productController  = new ProductController(productService, insumoService, catService);
        OrderController    orderController    = new OrderController(orderService, productService);
        SupplierController supplierController = new SupplierController(supplierService);
        PurchaseController purchaseController = new PurchaseController(purchaseService, supplierService, insumoService);
        ScheduleController scheduleController = new ScheduleController(scheduleService, userService);
        AiController       aiController       = new AiController(aiService);
        AuditController    auditController    = new AuditController(auditRepo);

        // 5. Middleware
        AuthMiddleware authMiddleware = new AuthMiddleware(userRepo);

        // 6. Javalin
        Javalin app = Javalin.create(config -> {
            // Arquivos estáticos (CSS, JS, imagens)
            config.staticFiles.add(staticFiles -> {
                staticFiles.hostedPath = "/static";
                staticFiles.directory  = "/static";
                staticFiles.location   = Location.CLASSPATH;
            });

            // Logging de requisições
            config.requestLogger.http((ctx, ms) ->
                log.info("{} {} → {} ({}ms) [{}]",
                        ctx.method(), ctx.path(), ctx.status(),
                        ms.intValue(), ctx.ip())
            );
        });

        // ====================================================================
        // Middleware: autenticação em todas as rotas
        // ====================================================================
        app.before(authMiddleware::handle);

        // ====================================================================
        // Rotas públicas
        // ====================================================================
        app.get("/login",   authController::showLogin);
        app.post("/login",  authController::processLogin);
        app.get("/register",  authController::showRegister);
        app.post("/register", authController::processRegister);
        app.get("/logout",  authController::logout);
        app.get("/ping",  ctx -> ctx.json(Map.of(
            "status", "ok",
            "service", "eq08",
            "timestamp", java.time.Instant.now().toString()
        )));

        // ====================================================================
        // Dashboard
        // ====================================================================
        app.get("/", dashboardCtrl::show);
        app.get("/dashboard", dashboardCtrl::show);

        // ====================================================================
        // Usuários
        // ====================================================================
        app.get("/usuarios",           userController::list);
        app.post("/usuarios",          userController::create);
        app.put("/usuarios/{id}",      userController::update);
        app.delete("/usuarios/{id}",   userController::delete);

        // ====================================================================
        // Categorias
        // ====================================================================
        app.get("/categorias",          catController::list);
        app.post("/categorias",         catController::create);
        app.delete("/categorias/{id}",  catController::delete);

        // ====================================================================
        // Insumos e Estoque
        // ====================================================================
        app.get("/insumos",                      insumoController::list);
        app.post("/insumos",                     insumoController::create);
        app.put("/insumos/{id}",                 insumoController::update);
        app.delete("/insumos/{id}",              insumoController::delete);
        app.post("/insumos/{id}/movimentacao",   insumoController::addMovement);
        app.get("/insumos/movimentacoes",        insumoController::listMovements);

        // ====================================================================
        // Produtos e Receitas
        // ====================================================================
        app.get("/produtos",          productController::list);
        app.post("/produtos",         productController::create);
        app.put("/produtos/{id}",     productController::update);
        app.delete("/produtos/{id}",  productController::delete);

        // ====================================================================
        // PDV / Vendas
        // ====================================================================
        app.get("/pdv",      orderController::showPdv);
        app.post("/vendas",  orderController::createOrder);
        app.get("/vendas",   orderController::listOrders);

        // ====================================================================
        // Fornecedores
        // ====================================================================
        app.get("/fornecedores",                      supplierController::list);
        app.post("/fornecedores",                     supplierController::create);
        app.put("/fornecedores/{id}",                 supplierController::update);
        app.delete("/fornecedores/{id}",              supplierController::delete);
        app.get("/fornecedores/{id}/performance",     supplierController::performance);

        // ====================================================================
        // Compras
        // ====================================================================
        app.get("/compras",              purchaseController::list);
        app.post("/compras",             purchaseController::create);
        app.post("/compras/{id}/concluir",  purchaseController::complete);
        app.post("/compras/{id}/cancelar",  purchaseController::cancel);

        // ====================================================================
        // Escalas, Trocas e Afastamentos
        // ====================================================================
        app.get("/escalas",                          scheduleController::listSchedules);
        app.post("/escalas",                         scheduleController::createSchedule);
        app.delete("/escalas/{id}",                  scheduleController::deleteSchedule);

        app.get("/escalas/trocas",                   scheduleController::listTrades);
        app.post("/escalas/trocas",                  scheduleController::createTrade);
        app.post("/escalas/trocas/{id}/aprovar",     scheduleController::approveTrade);
        app.post("/escalas/trocas/{id}/rejeitar",    scheduleController::rejectTrade);

        app.get("/escalas/afastamentos",             scheduleController::listAbsences);
        app.post("/escalas/afastamentos",            scheduleController::createAbsence);
        app.post("/escalas/afastamentos/{id}/aprovar", scheduleController::approveAbsence);

        // ====================================================================
        // Analytics / IA
        // ====================================================================
        app.get("/analytics",                          aiController::showAnalytics);
        app.post("/analytics/gerar-previsoes",         aiController::generateForecasts);
        app.post("/analytics/recomendacoes/{id}/aplicar",  aiController::applyRecommendation);
        app.post("/analytics/recomendacoes/{id}/dispensar", aiController::dismissRecommendation);

        // ====================================================================
        // Auditoria
        // ====================================================================
        app.get("/auditoria", auditController::list);

        // ====================================================================
        // Tratamento de erros
        // ====================================================================
        app.exception(AppException.class, (e, ctx) -> {
            log.warn("AppException [{}]: {}", e.getStatusCode(), e.getMessage());
            if (isHtmx(ctx)) {
                ctx.status(e.getStatusCode());
                ctx.html("<div class=\"alert alert-danger alert-dismissible\" role=\"alert\">" +
                         "<i class=\"bi bi-exclamation-triangle-fill me-2\"></i>" +
                         e.getMessage() +
                         "<button type=\"button\" class=\"btn-close\" data-bs-dismiss=\"alert\"></button>" +
                         "</div>");
            } else {
                ctx.status(e.getStatusCode());
                ctx.render("templates/error.html", Map.of(
                        "statusCode", e.getStatusCode(),
                        "message", e.getMessage()
                ));
            }
        });

        app.exception(Exception.class, (e, ctx) -> {
            log.error("Erro inesperado: {}", e.getMessage(), e);
            if (isHtmx(ctx)) {
                ctx.status(500);
                ctx.html("<div class=\"alert alert-danger\">Erro interno do servidor. Tente novamente.</div>");
            } else {
                ctx.status(500);
                ctx.render("templates/error.html", Map.of(
                        "statusCode", 500,
                        "message", "Erro interno do servidor."
                ));
            }
        });

        // ====================================================================
        // Iniciar servidor
        // ====================================================================
        app.start(AppConfig.APP_PORT);
        log.info("=== Servidor iniciado na porta {} ===", AppConfig.APP_PORT);
        log.info("URL: http://localhost:{}", AppConfig.APP_PORT);

        // Shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Encerrando aplicação...");
            app.stop();
            DatabaseConfig.close();
        }));
    }

    private static boolean isHtmx(io.javalin.http.Context ctx) {
        return ctx.header("HX-Request") != null;
    }
}
