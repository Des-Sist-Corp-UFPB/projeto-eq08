package br.ufpb.eq08.gestor.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Scorecard de performance de um fornecedor.
 */
public record SupplierPerformance(
        UUID supplierId,
        String supplierName,
        double avgDeliveryDays,
        double avgQualityRating,
        double avgPriceRating,
        BigDecimal totalPurchasesValue,
        long purchaseOrdersCount
) {}
