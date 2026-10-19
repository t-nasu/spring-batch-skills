package com.example.orderimport.infrastructure.datasource.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * {@code orders} の 1 行。
 *
 * <p>ドメインモデルを DB の都合から切り離すための変換用オブジェクト。
 * {@code amount} は単価 × 数量の計算結果で、日次集計を SQL 側で行えるように
 * あらかじめ保持している（導出項目の冗長保持）。
 */
public record OrderRow(
        String orderNumber,
        LocalDate businessDate,
        String customerCode,
        String productCode,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal amount,
        LocalDateTime orderedAt,
        LocalDateTime createdAt) {
}
