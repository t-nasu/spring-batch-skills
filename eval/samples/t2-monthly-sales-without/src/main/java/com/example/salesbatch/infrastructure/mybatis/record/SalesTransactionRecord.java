package com.example.salesbatch.infrastructure.mybatis.record;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * {@code sales_transaction} の 1 行に対応するレコード（MyBatis のマッピング対象）。
 *
 * <p>不正データも含めてそのまま読み込むため、検証は行わない。</p>
 */
public record SalesTransactionRecord(
        long transactionId,
        String storeId,
        String productId,
        LocalDate transactionDate,
        String type,
        BigDecimal amount,
        Integer quantity) {
}
