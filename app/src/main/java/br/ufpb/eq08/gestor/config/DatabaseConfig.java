package br.ufpb.eq08.gestor.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * Configuração e inicialização do banco de dados.
 * Gerencia o pool de conexões HikariCP e executa migrações Flyway.
 */
public final class DatabaseConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConfig.class);
    private static HikariDataSource dataSource;

    private DatabaseConfig() {}

    /**
     * Inicializa o pool de conexões HikariCP.
     * Deve ser chamado uma única vez na inicialização da aplicação.
     */
    public static DataSource init() {
        if (dataSource != null) {
            return dataSource;
        }

        log.info("Inicializando pool de conexões HikariCP...");
        log.info("DB_URL: {}", AppConfig.DB_URL);

        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(AppConfig.DB_URL);
        config.setUsername(AppConfig.DB_USER);
        config.setPassword(AppConfig.DB_PASSWORD);
        config.setDriverClassName("org.postgresql.Driver");

        // Pool settings
        config.setMaximumPoolSize(AppConfig.POOL_MAX_SIZE);
        config.setMinimumIdle(AppConfig.POOL_MIN_IDLE);
        config.setConnectionTimeout(AppConfig.CONN_TIMEOUT_MS);
        config.setIdleTimeout(AppConfig.IDLE_TIMEOUT_MS);
        config.setPoolName("GestorPool");

        // Validação de conexão
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5_000L);

        dataSource = new HikariDataSource(config);
        log.info("Pool HikariCP inicializado com sucesso.");

        // Executar migrações Flyway
        runMigrations(dataSource);

        return dataSource;
    }

    /**
     * Executa as migrações Flyway no banco de dados.
     * Scripts de migração estão em src/main/resources/db/migration/
     */
    private static void runMigrations(DataSource ds) {
        log.info("Executando migrações Flyway...");
        Flyway flyway = Flyway.configure()
                .dataSource(ds)
                .locations("classpath:db/migration")
                .baselineOnMigrate(true)
                .validateOnMigrate(true)
                .load();

        var result = flyway.migrate();
        log.info("Flyway: {} migrações aplicadas com sucesso.", result.migrationsExecuted);
    }

    /**
     * Retorna o DataSource inicializado.
     */
    public static DataSource getDataSource() {
        if (dataSource == null) {
            throw new IllegalStateException("DatabaseConfig.init() não foi chamado ainda.");
        }
        return dataSource;
    }

    /**
     * Fecha o pool de conexões graciosamente.
     */
    public static void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            log.info("Fechando pool de conexões HikariCP...");
            dataSource.close();
        }
    }
}
