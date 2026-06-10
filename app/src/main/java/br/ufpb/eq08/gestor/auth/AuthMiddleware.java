package br.ufpb.eq08.gestor.auth;

import br.ufpb.eq08.gestor.config.AppConfig;
import br.ufpb.eq08.gestor.domain.User;
import br.ufpb.eq08.gestor.repository.UserRepository;
import io.javalin.http.Context;
import io.javalin.http.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * Middleware de autenticação e autorização.
 *
 * - Verifica o cookie JWT em cada requisição protegida.
 * - Extrai o usuário autenticado e o disponibiliza em ctx.attribute("currentUser").
 * - Redireciona para /login se o token for inválido ou ausente.
 */
public class AuthMiddleware {

    private static final Logger log = LoggerFactory.getLogger(AuthMiddleware.class);

    /** Rotas públicas (não precisam de autenticação) */
    private static final Set<String> PUBLIC_PATHS = new HashSet<>(Arrays.asList(
            "/login", "/register", "/health", "/static"
    ));

    private final UserRepository userRepository;

    public AuthMiddleware(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Handler de before() para verificar autenticação em todas as rotas.
     */
    public void handle(Context ctx) {
        String path = ctx.path();

        // Liberar rotas públicas e arquivos estáticos
        if (isPublicPath(path)) {
            return;
        }

        String token = ctx.cookie(AppConfig.JWT_COOKIE_NAME);
        UUID userId = JwtUtil.extractUserId(token);

        if (userId == null) {
            log.debug("Requisição sem token válido para: {} — redirecionando para /login", path);
            if (isHtmxRequest(ctx)) {
                ctx.header("HX-Redirect", "/login");
                ctx.status(401);
            } else {
                ctx.redirect("/login");
            }
            return;
        }

        // Carregar usuário completo do banco
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || !user.isActive()) {
            log.warn("Usuário {} não encontrado ou inativo — limpando cookie.", userId);
            ctx.removeCookie(AppConfig.JWT_COOKIE_NAME);
            ctx.redirect("/login");
            return;
        }

        // Armazenar usuário no contexto da requisição
        ctx.attribute("currentUser", user);
        log.debug("Usuário autenticado: {} [{}] — {}", user.email(), user.role(), path);
    }

    /**
     * Verificar se uma rota requer role específico.
     * Uso: AuthMiddleware.requireRole(ctx, "OWNER", "MANAGER");
     */
    public static void requireRole(Context ctx, String... allowedRoles) {
        User user = ctx.attribute("currentUser");
        if (user == null) {
            throw new br.ufpb.eq08.gestor.exception.AppException(401, "Não autenticado.");
        }
        for (String role : allowedRoles) {
            if (user.role().equals(role)) return;
        }
        throw new br.ufpb.eq08.gestor.exception.AppException(
                403,
                "Permissão insuficiente. Requer: " + Arrays.toString(allowedRoles)
        );
    }

    /**
     * Retorna o usuário atual do contexto. Lança 401 se não autenticado.
     */
    public static User currentUser(Context ctx) {
        User user = ctx.attribute("currentUser");
        if (user == null) {
            throw new br.ufpb.eq08.gestor.exception.AppException(401, "Não autenticado.");
        }
        return user;
    }

    private boolean isPublicPath(String path) {
        return PUBLIC_PATHS.stream().anyMatch(path::startsWith);
    }

    private boolean isHtmxRequest(Context ctx) {
        return ctx.header("HX-Request") != null;
    }
}
