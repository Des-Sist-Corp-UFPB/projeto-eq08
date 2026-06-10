package br.ufpb.eq08.gestor.config;

/**
 * Configuração da aplicação a partir de variáveis de ambiente.
 * Leitura centralizada de todas as configs, sem uso de arquivos .properties
 * para facilitar deploy via Docker/Portainer.
 */
public final class AppConfig {

    // ========================
    // Banco de dados
    // ========================
    public static final String DB_URL      = getEnv("DB_URL", "jdbc:postgresql://localhost:5432/gestor_db");
    public static final String DB_USER     = getEnv("DB_USER", "gestor_user");
    public static final String DB_PASSWORD = getEnv("DB_PASSWORD", "gestor_password");

    // ========================
    // Segurança JWT
    // ========================
    public static final String JWT_SECRET      = getEnv("JWT_SECRET", "mude-esta-chave-secreta-em-producao-1234567890abcdef");
    public static final int    JWT_EXPIRY_DAYS = Integer.parseInt(getEnv("JWT_EXPIRY_DAYS", "7"));

    // ========================
    // Servidor
    // ========================
    public static final int    APP_PORT = Integer.parseInt(getEnv("APP_PORT", "7070"));
    public static final String APP_ENV  = getEnv("APP_ENV", "development");

    // ========================
    // HikariCP Pool
    // ========================
    public static final int POOL_MAX_SIZE   = Integer.parseInt(getEnv("DB_POOL_MAX", "10"));
    public static final int POOL_MIN_IDLE   = Integer.parseInt(getEnv("DB_POOL_MIN", "2"));
    public static final long CONN_TIMEOUT_MS = 30_000L;
    public static final long IDLE_TIMEOUT_MS = 600_000L;

    // ========================
    // Nome do cookie JWT
    // ========================
    public static final String JWT_COOKIE_NAME = "gestor_jwt";

    private AppConfig() {
        // Utilitário — não instanciar
    }

    private static String getEnv(String key, String defaultValue) {
        String value = System.getenv(key);
        return (value != null && !value.isBlank()) ? value : defaultValue;
    }

    public static boolean isProduction() {
        return "production".equalsIgnoreCase(APP_ENV);
    }
}
