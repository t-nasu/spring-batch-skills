package com.example.monthlysales.infrastructure.summary;

import java.math.BigDecimal;

/** monthly_sales_summary 表の 1 行をそのまま写した DTO。 */
public record MonthlySalesSummaryRow(
        String targetMonth,
        String storeId,
        String productId,
        BigDecimal totalAmount,
        long totalQuantity) {
}
