package br.ufpb.eq08.gestor.service;

import br.ufpb.eq08.gestor.domain.AIRecommendation;
import br.ufpb.eq08.gestor.domain.DemandForecast;
import br.ufpb.eq08.gestor.domain.Insumo;
import br.ufpb.eq08.gestor.repository.AiRepository;
import br.ufpb.eq08.gestor.repository.InsumoRepository;
import br.ufpb.eq08.gestor.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Serviço de IA simplificado:
 * - Previsão de demanda via média móvel dos últimos 30 dias
 * - Recomendações de reabastecimento para insumos abaixo do mínimo
 */
public class AiService {

    private static final Logger log = LoggerFactory.getLogger(AiService.class);

    private final AiRepository aiRepo;
    private final InsumoRepository insumoRepo;
    private final SupplierRepository supplierRepo;

    public AiService(AiRepository aiRepo, InsumoRepository insumoRepo, SupplierRepository supplierRepo) {
        this.aiRepo        = aiRepo;
        this.insumoRepo    = insumoRepo;
        this.supplierRepo  = supplierRepo;
    }

    /** Retorna previsões existentes (futuras) para o tenant. */
    public List<DemandForecast> getForecasts(UUID tenantId) {
        return aiRepo.findForecastsByTenant(tenantId);
    }

    /** Retorna recomendações pendentes. */
    public List<AIRecommendation> getPendingRecommendations(UUID tenantId) {
        return aiRepo.findPendingByTenant(tenantId);
    }

    public List<AIRecommendation> getAllRecommendations(UUID tenantId) {
        return aiRepo.findAllByTenant(tenantId);
    }

    /**
     * Gera previsões de demanda para os próximos 7 dias
     * via média móvel dos últimos 30 dias de vendas.
     */
    public void generateForecasts(UUID tenantId) {
        log.info("Gerando previsões para tenant {}", tenantId);

        List<Object[]> stats = aiRepo.getSalesStats(tenantId, 30);
        if (stats.isEmpty()) {
            log.info("Sem dados históricos para gerar previsões.");
            return;
        }

        // Calcular médias
        double avgOrders = stats.stream()
                .mapToLong(row -> (Long) row[1])
                .average().orElse(0);

        BigDecimal totalRev = stats.stream()
                .map(row -> (BigDecimal) row[2])
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal avgRevenue = totalRev.divide(BigDecimal.valueOf(Math.max(stats.size(), 1)), 2, RoundingMode.HALF_UP);

        // Calcular desvio padrão para confidence score
        double variance = stats.stream()
                .mapToDouble(row -> {
                    double diff = (Long) row[1] - avgOrders;
                    return diff * diff;
                }).average().orElse(0);
        double stddev = Math.sqrt(variance);
        double confidence = Math.max(0.0, Math.min(1.0, 1.0 - (stddev / Math.max(avgOrders, 1))));

        // Gerar previsão para os próximos 7 dias
        for (int i = 1; i <= 7; i++) {
            LocalDate targetDate = LocalDate.now().plusDays(i);
            aiRepo.upsertForecast(tenantId, targetDate,
                    (int) Math.round(avgOrders),
                    avgRevenue,
                    BigDecimal.valueOf(confidence).setScale(2, RoundingMode.HALF_UP));
        }

        log.info("Previsões geradas para os próximos 7 dias.");
    }

    /**
     * Analisa insumos abaixo do mínimo e cria recomendações de reabastecimento.
     */
    public void generateStockRecommendations(UUID tenantId) {
        List<Insumo> belowMin = insumoRepo.findBelowMinimum(tenantId);

        for (Insumo insumo : belowMin) {
            String title = "Reabastecimento necessário: " + insumo.name();
            String description = String.format(
                    "O insumo '%s' está com estoque de %.4f %s, " +
                    "abaixo do mínimo de %.4f %s. " +
                    "Recomenda-se realizar pedido de compra.",
                    insumo.name(),
                    insumo.currentStock(), insumo.unit(),
                    insumo.minimumStock(), insumo.unit()
            );

            // Nível de impacto baseado em quanto está abaixo do mínimo
            double ratio = insumo.minimumStock().compareTo(BigDecimal.ZERO) == 0 ? 0
                    : insumo.currentStock().doubleValue() / insumo.minimumStock().doubleValue();
            String impact = ratio < 0.25 ? "HIGH" : ratio < 0.5 ? "MEDIUM" : "LOW";

            String actionData = String.format(
                    "{\"insumo_id\":\"%s\",\"insumo_name\":\"%s\",\"current_stock\":%.4f,\"minimum_stock\":%.4f,\"unit\":\"%s\"}",
                    insumo.id(), insumo.name(), insumo.currentStock(), insumo.minimumStock(), insumo.unit()
            );

            // Criar recomendação apenas se não houver outra pendente para o mesmo insumo
            boolean alreadyExists = getPendingRecommendations(tenantId).stream()
                    .anyMatch(r -> r.type().equals("STOCK_REPLENISHMENT") &&
                                  r.actionData() != null && r.actionData().contains(insumo.id().toString()));

            if (!alreadyExists) {
                aiRepo.create(tenantId, "STOCK_REPLENISHMENT", title, description, impact, actionData);
                log.info("Recomendação criada para insumo: {} [{}]", insumo.name(), impact);
            }
        }
    }

    public void applyRecommendation(UUID id, UUID tenantId) {
        aiRepo.findById(id)
                .filter(r -> r.tenantId().equals(tenantId))
                .orElseThrow(() -> new br.ufpb.eq08.gestor.exception.AppException(404, "Recomendação não encontrada."));
        aiRepo.updateStatus(id, "APPLIED");
    }

    public void dismissRecommendation(UUID id, UUID tenantId) {
        aiRepo.findById(id)
                .filter(r -> r.tenantId().equals(tenantId))
                .orElseThrow(() -> new br.ufpb.eq08.gestor.exception.AppException(404, "Recomendação não encontrada."));
        aiRepo.updateStatus(id, "DISMISSED");
    }

    /** Conta recomendações pendentes de HIGH impact (para dashboard). */
    public long countHighImpactRecommendations(UUID tenantId) {
        return getPendingRecommendations(tenantId).stream()
                .filter(r -> "HIGH".equals(r.impactLevel()))
                .count();
    }
}
