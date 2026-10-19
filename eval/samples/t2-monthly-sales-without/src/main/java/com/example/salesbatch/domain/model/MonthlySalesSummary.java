package com.example.salesbatch.domain.model;

import java.util.Objects;

/**
 * 月次売上集計（店舗 × 商品 × 対象年月）を表す集約。出力表 {@code monthly_sales_summary} の 1 行に対応する。
 *
 * @param targetMonth      対象年月
 * @param groupKey         集計単位（店舗 × 商品）
 * @param totalAmount      金額合計（返品は差し引き済み）
 * @param totalQuantity    数量合計（返品は差し引き済み）
 * @param transactionCount 集計に含めた取引明細の件数（不正データとして読み飛ばした分は含まない）
 */
public record MonthlySalesSummary(
        TargetMonth targetMonth,
        SalesGroupKey groupKey,
        Money totalAmount,
        Quantity totalQuantity,
        int transactionCount) {

    public MonthlySalesSummary {
        Objects.requireNonNull(targetMonth, "targetMonth は必須です");
        Objects.requireNonNull(groupKey, "groupKey は必須です");
        Objects.requireNonNull(totalAmount, "totalAmount は必須です");
        Objects.requireNonNull(totalQuantity, "totalQuantity は必須です");
        if (transactionCount < 0) {
            throw new IllegalArgumentException("transactionCount は 0 以上です: " + transactionCount);
        }
    }

    public StoreId storeId() {
        return groupKey.storeId();
    }

    public ProductId productId() {
        return groupKey.productId();
    }
}
