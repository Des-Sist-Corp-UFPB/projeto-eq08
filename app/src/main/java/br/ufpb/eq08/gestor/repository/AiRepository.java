package br.ufpb.eq08.gestor.repository;

import br.ufpb.eq08.gestor.domain.AIRecommendation;
import br.ufpb.eq08.gestor.domain.DemandForecast;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositório para Previsões de Demanda e Recomendações de IA.
 */
public class AiRepository {

    private static final Logger log = LoggerFactory.getLogger(AiRepository.class);
    private final DataSource ds;

    public AiRepository(DataSource ds) {
        this.ds = ds;
    }

    // ====================================================================
    // DEMAND FORECASTS
    // ====================================================================

    public List<DemandForecast> findForecastsByTenant(UUID tenantId) {
        String sql = """
                SELECT * FROM demand_forecasts
                WHERE tenant_id = ? AND target_date >= CURRENT_DATE
                ORDER BY target_date
                """;
        List<DemandForecast> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(DemandForecast.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar previsões: {}", e.getMessage());
        }
        return list;
    }

    public void upsertForecast(UUID tenantId, LocalDate targetDate, int predictedOrders,
                                BigDecimal predictedRevenue, BigDecimal confidenceScore) {
        String sql = """
                INSERT INTO demand_forecasts (tenant_id, target_date, predicted_orders, predicted_revenue, confidence_score, model_version)
                VALUES (?, ?, ?, ?, ?, '1.0')
                ON CONFLICT (tenant_id, target_date) DO UPDATE
                SET predicted_orders = EXCLUDED.predicted_orders,
                    predicted_revenue = EXCLUDED.predicted_revenue,
                    confidence_score = EXCLUDED.confidence_score,
                    created_at = NOW()
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setObject(2, targetDate);
            stmt.setInt(3, predictedOrders);
            stmt.setBigDecimal(4, predictedRevenue);
            stmt.setBigDecimal(5, confidenceScore);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao salvar previsão: {}", e.getMessage());
        }
    }

    // ====================================================================
    // AI RECOMMENDATIONS
    // ====================================================================

    public List<AIRecommendation> findPendingByTenant(UUID tenantId) {
        String sql = """
                SELECT * FROM ai_recommendations
                WHERE tenant_id = ? AND status = 'PENDING'
                ORDER BY
                    CASE impact_level WHEN 'HIGH' THEN 1 WHEN 'MEDIUM' THEN 2 ELSE 3 END,
                    created_at DESC
                """;
        List<AIRecommendation> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(AIRecommendation.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar recomendações: {}", e.getMessage());
        }
        return list;
    }

    public List<AIRecommendation> findAllByTenant(UUID tenantId) {
        String sql = "SELECT * FROM ai_recommendations WHERE tenant_id = ? ORDER BY created_at DESC LIMIT 50";
        List<AIRecommendation> list = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) list.add(AIRecommendation.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao listar recomendações: {}", e.getMessage());
        }
        return list;
    }

    public Optional<AIRecommendation> findById(UUID id) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement("SELECT * FROM ai_recommendations WHERE id = ?")) {
            stmt.setObject(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return Optional.of(AIRecommendation.fromResultSet(rs));
        } catch (SQLException e) {
            log.error("Erro ao buscar recomendação: {}", e.getMessage());
        }
        return Optional.empty();
    }

    public AIRecommendation create(UUID tenantId, String type, String title, String description,
                                    String impactLevel, String actionData) {
        String sql = """
                INSERT INTO ai_recommendations (tenant_id, type, title, description, impact_level, action_data, status)
                VALUES (?, ?, ?, ?, ?, ?::jsonb, 'PENDING') RETURNING *
                """;
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            stmt.setString(2, type);
            stmt.setString(3, title);
            stmt.setString(4, description);
            stmt.setString(5, impactLevel);
            stmt.setString(6, actionData);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) return AIRecommendation.fromResultSet(rs);
        } catch (SQLException e) {
            log.error("Erro ao criar recomendação: {}", e.getMessage());
            throw new RuntimeException("Erro ao criar recomendação.", e);
        }
        throw new RuntimeException("Erro inesperado.");
    }

    public void updateStatus(UUID id, String status) {
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(
                     "UPDATE ai_recommendations SET status = ?, updated_at = NOW() WHERE id = ?")) {
            stmt.setString(1, status);
            stmt.setObject(2, id);
            stmt.executeUpdate();
        } catch (SQLException e) {
            log.error("Erro ao atualizar status de recomendação: {}", e.getMessage());
            throw new RuntimeException("Erro ao atualizar recomendação.", e);
        }
    }

    /** Estatísticas de vendas dos últimos N dias para o motor de previsão. */
    public List<Object[]> getSalesStats(UUID tenantId, int days) {
        String sql = """
                SELECT
                    DATE(created_at) AS sale_date,
                    COUNT(*) AS order_count,
                    COALESCE(SUM(total_price), 0) AS revenue
                FROM orders
                WHERE tenant_id = ? AND created_at >= NOW() - INTERVAL '%d days'
                GROUP BY DATE(created_at)
                ORDER BY sale_date
                """.formatted(days);
        List<Object[]> rows = new ArrayList<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setObject(1, tenantId);
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                rows.add(new Object[]{
                        rs.getObject("sale_date", LocalDate.class),
                        rs.getLong("order_count"),
                        rs.getBigDecimal("revenue")
                });
            }
        } catch (SQLException e) {
            log.error("Erro ao buscar estatísticas de vendas: {}", e.getMessage());
        }
        return rows;
    }
}
