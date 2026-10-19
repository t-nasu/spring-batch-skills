package com.example.monthlysales.infrastructure.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * sales_transaction 表の 1 行をそのまま写した DTO。
 *
 * <p>DB に壊れた値が入っていても読み出せるよう、ここでは一切検査しない。
 * 業務上の決まりの検査はドメインオブジェクトへ変換するときに行う。
 */
public record SalesTransactionRow(
        String transactionId,
        String storeId,
        String productId,
        LocalDate transactionDate,
        String type,
        BigDecimal amount,
        Long quantity) {
}
