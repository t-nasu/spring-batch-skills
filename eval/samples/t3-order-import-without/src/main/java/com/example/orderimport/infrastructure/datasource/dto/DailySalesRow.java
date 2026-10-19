package com.example.orderimport.infrastructure.datasource.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code daily_sales} の 1 行、および {@code orders} の集計結果。
 *
 * <p>集計クエリの結果を受けるときは {@code aggregatedAt} が null になる。
 * 登録時に {@link com.example.orderimport.infrastructure.datasource.MyBatisDailySalesRepository}
 * が値を補う。
 */
public record DailySalesRow(
        LocalDate businessDate,
        String productCode,
        int orderCount,
        long totalQuantity,
        BigDecimal totalAmount,
        LocalDateTime aggregatedAt) {
}
