package br.ufpb.eq08.gestor.exception;

/**
 * Exceção de negócio da aplicação.
 * Carrega status HTTP e mensagem amigável para o usuário.
 */
public class AppException extends RuntimeException {

    private final int statusCode;

    public AppException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }

    // ── Fábricas de conveniência ──────────────────────────────────────────────

    public static AppException notFound(String message) {
        return new AppException(404, message);
    }

    public static AppException badRequest(String message) {
        return new AppException(400, message);
    }

    public static AppException unauthorized(String message) {
        return new AppException(401, message);
    }

    public static AppException forbidden(String message) {
        return new AppException(403, message);
    }

    public static AppException conflict(String message) {
        return new AppException(409, message);
    }

    public static AppException internalError(String message) {
        return new AppException(500, message);
    }
}
