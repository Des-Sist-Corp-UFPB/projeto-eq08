package br.ufpb.eq08.gestor.controller;

import br.ufpb.eq08.gestor.config.AppConfig;
import br.ufpb.eq08.gestor.service.AuthService;
import io.javalin.http.Context;
import io.javalin.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller de autenticação: login, registro de tenant e logout.
 * Usa Thymeleaf para renderização das páginas.
 */
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);
    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** GET /login */
    public void showLogin(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("pageTitle", "Login");
        ctx.render("templates/login.html", model);
    }

    /** POST /login */
    public void processLogin(Context ctx) {
        String email    = ctx.formParam("email");
        String password = ctx.formParam("password");

        try {
            String token = authService.login(email, password, ctx.ip());

            // Definir cookie JWT httpOnly
            Cookie jwtCookie = new Cookie(AppConfig.JWT_COOKIE_NAME, token);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(AppConfig.JWT_EXPIRY_DAYS * 24 * 3600);
            if (AppConfig.isProduction()) {
                jwtCookie.setSecure(true);
                jwtCookie.setSameSite(io.javalin.http.SameSite.STRICT);
            }
            ctx.cookie(jwtCookie);

            // Redirecionar para dashboard
            ctx.redirect("/");

        } catch (Exception e) {
            Map<String, Object> model = new HashMap<>();
            model.put("pageTitle", "Login");
            model.put("error", e.getMessage());
            model.put("email", email);
            ctx.status(400).render("templates/login.html", model);
        }
    }

    /** GET /register */
    public void showRegister(Context ctx) {
        Map<String, Object> model = new HashMap<>();
        model.put("pageTitle", "Registrar Empresa");
        ctx.render("templates/register.html", model);
    }

    /** POST /register */
    public void processRegister(Context ctx) {
        String companyName  = ctx.formParam("companyName");
        String slug         = ctx.formParam("slug");
        String adminName    = ctx.formParam("adminName");
        String adminEmail   = ctx.formParam("adminEmail");
        String adminPassword = ctx.formParam("adminPassword");

        try {
            authService.registerTenant(companyName, slug, adminName, adminEmail, adminPassword);
            ctx.redirect("/login?registered=true");
        } catch (Exception e) {
            Map<String, Object> model = new HashMap<>();
            model.put("pageTitle", "Registrar Empresa");
            model.put("error", e.getMessage());
            model.put("companyName", companyName);
            model.put("slug", slug);
            model.put("adminName", adminName);
            model.put("adminEmail", adminEmail);
            ctx.status(400).render("templates/register.html", model);
        }
    }

    /** GET /logout */
    public void logout(Context ctx) {
        ctx.removeCookie(AppConfig.JWT_COOKIE_NAME);
        ctx.redirect("/login");
    }
}
