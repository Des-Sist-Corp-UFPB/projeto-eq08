package br.ufpb.eq08.gestor.controller;

import io.javalin.http.Context;

import java.util.Map;

/**
 * Utilitários compartilhados por todos os controllers.
 */
public final class ControllerHelpers {

    private ControllerHelpers() {}

    /**
     * Se a requisição vier do HTMX, renderiza o fragment; caso contrário redireciona.
     */
    public static void htmxOrRedirect(Context ctx, String redirectPath,
                                      String fragmentTemplate, Map<String, Object> model) {
        if (ctx.header("HX-Request") != null) {
            ctx.render(fragmentTemplate, model);
        } else {
            ctx.redirect(redirectPath);
        }
    }

    /**
     * Renderiza a página completa ou apenas o fragment HTMX.
     */
    public static void renderOrFragment(Context ctx, String fullPage,
                                        String fragment, Map<String, Object> model) {
        if (ctx.header("HX-Request") != null) {
            ctx.render(fragment, model);
        } else {
            ctx.render(fullPage, model);
        }
    }

    /** Retorna o valor padrão se o string for null ou blank. */
    public static String defaultStr(String s, String def) {
        return (s == null || s.isBlank()) ? def : s;
    }
}
