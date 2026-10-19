package com.example.salesbatch.infrastructure.mybatis.record;

import java.math.BigDecimal;

/**
 * {@code monthly_sales_summary} の 1 行に対応するレコード（MyBatis のマッピング対象）。
 */
public record MonthlySalesSummaryRecord(
        String targetMonth,
        String storeId,
        String productId,
        BigDecimal totalAmount,
        int totalQuantity,
        int transactionCount) {
}
