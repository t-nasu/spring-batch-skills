package com.example.salesbatch.domain.model;

import com.example.salesbatch.domain.exception.InvalidSalesTransactionException;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 未検証の取引明細。入力表 {@code sales_transaction} の 1 行をそのまま写したもの。
 *
 * <p>入力表には不正データが混ざりうるため、値オブジェクトに変換する前段として
 * 生の値を保持する型を置き、{@link #toSalesTransaction()} で検証して
 * {@link SalesTransaction} に変換する（変換できないものが「不正データ」）。</p>
 *
 * <p>{@code storeId} / {@code productId} / {@code transactionId} は
 * 読み込み位置（キーセットページング）にも使うため、検証前の生の値を保持する。
 * 対応する列は NOT NULL であり、null にはならない。</p>
 */
public record RawSalesTransaction(
        long transactionId,
        String storeId,
        String productId,
        LocalDate transactionDate,
        String type,
        BigDecimal amount,
        Integer quantity) {

    /**
     * 検証して {@link SalesTransaction} に変換する。
     *
     * @throws InvalidSalesTransactionException 集計に使えない値が含まれる場合
     */
    public SalesTransaction toSalesTransaction() {
        SalesGroupKey groupKey;
        try {
            groupKey = SalesGroupKey.of(storeId, productId);
        } catch (IllegalArgumentException e) {
            throw invalid(e.getMessage(), e);
        }

        TransactionType transactionType;
        try {
            transactionType = TransactionType.fromCode(type);
        } catch (IllegalArgumentException e) {
            throw invalid(e.getMessage(), e);
        }

        if (transactionDate == null) {
            throw invalid("取引日が未設定です");
        }
        if (amount == null) {
            throw invalid("金額が未設定です");
        }
        if (amount.signum() < 0) {
            throw invalid("金額が負数です: " + amount.toPlainString());
        }
        if (quantity == null) {
            throw invalid("数量が未設定です");
        }
        if (quantity <= 0) {
            throw invalid("数量が 0 以下です: " + quantity);
        }

        return new SalesTransaction(
                TransactionId.of(transactionId),
                groupKey,
                transactionType,
                Money.of(amount),
                Quantity.of(quantity),
                transactionDate);
    }

    private InvalidSalesTransactionException invalid(String reason) {
        return new InvalidSalesTransactionException(transactionId, reason);
    }

    private InvalidSalesTransactionException invalid(String reason, Throwable cause) {
        return new InvalidSalesTransactionException(transactionId, reason, cause);
    }
}
