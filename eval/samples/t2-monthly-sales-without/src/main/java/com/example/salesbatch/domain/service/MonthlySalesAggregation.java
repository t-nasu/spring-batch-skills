package com.example.salesbatch.domain.service;

import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.Money;
import com.example.salesbatch.domain.model.Quantity;
import com.example.salesbatch.domain.model.SalesGroupKey;
import com.example.salesbatch.domain.model.SalesTransaction;
import com.example.salesbatch.domain.model.TargetMonth;

import java.util.Objects;

/**
 * 1 つの集計単位（店舗 × 商品）について、取引明細を 1 件ずつ足し込んで
 * {@link MonthlySalesSummary} を組み立てるドメインサービス。
 *
 * <p>売上は加算、返品（{@code type = 'RETURN'}）は減算する。
 * 件数規模（最大 800 万件）に耐えるため明細を溜め込まず、逐次加算する。</p>
 */
public final class MonthlySalesAggregation {

    private final TargetMonth targetMonth;
    private final SalesGroupKey groupKey;

    private Money totalAmount = Money.ZERO;
    private Quantity totalQuantity = Quantity.ZERO;
    private int transactionCount;

    public MonthlySalesAggregation(TargetMonth targetMonth, SalesGroupKey groupKey) {
        this.targetMonth = Objects.requireNonNull(targetMonth, "targetMonth は必須です");
        this.groupKey = Objects.requireNonNull(groupKey, "groupKey は必須です");
    }

    /** 集計単位。 */
    public SalesGroupKey groupKey() {
        return groupKey;
    }

    /**
     * 取引明細を 1 件足し込む。
     *
     * @throws IllegalArgumentException 集計単位が一致しない明細を渡した場合
     */
    public void add(SalesTransaction transaction) {
        Objects.requireNonNull(transaction, "transaction は必須です");
        if (!groupKey.equals(transaction.groupKey())) {
            throw new IllegalArgumentException(
                    "集計単位が一致しません: expected=%s actual=%s".formatted(groupKey, transaction.groupKey()));
        }
        totalAmount = totalAmount.add(transaction.signedAmount());
        totalQuantity = totalQuantity.add(transaction.signedQuantity());
        transactionCount++;
    }

    /** 足し込んだ明細が 1 件もないか。 */
    public boolean isEmpty() {
        return transactionCount == 0;
    }

    /** 現時点の集計結果を確定させる。 */
    public MonthlySalesSummary toSummary() {
        return new MonthlySalesSummary(targetMonth, groupKey, totalAmount, totalQuantity, transactionCount);
    }
}
