package com.example.monthlysales.domain.summary;

import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.Money;
import com.example.monthlysales.domain.shared.ProductId;
import com.example.monthlysales.domain.shared.Quantity;
import com.example.monthlysales.domain.shared.StoreId;
import com.example.monthlysales.domain.shared.TargetMonth;
import java.util.Objects;

/**
 * 店舗・商品ごとの月次売上集計。
 *
 * <p>取引 1 件だけを計上したものも、多数を足し合わせたものも同じ型で表す。
 * 足し合わせは {@link #add(MonthlySalesSummary)} で行い、合計の求め方を
 * ここに閉じ込める（SQL では合計しない）。
 */
public final class MonthlySalesSummary {

    private final MonthlySalesKey key;
    private final Money totalAmount;
    private final Quantity totalQuantity;

    private MonthlySalesSummary(MonthlySalesKey key, Money totalAmount, Quantity totalQuantity) {
        this.key = Objects.requireNonNull(key, "識別子が無い");
        this.totalAmount = Objects.requireNonNull(totalAmount, "合計金額が無い");
        this.totalQuantity = Objects.requireNonNull(totalQuantity, "合計数量が無い");
    }

    /** 取引 1 件だけを計上した集計を作る。返品は金額・数量とも差し引かれる。 */
    public static MonthlySalesSummary of(TargetMonth targetMonth, SalesTransaction transaction) {
        MonthlySalesKey key =
                new MonthlySalesKey(targetMonth, transaction.storeId(), transaction.productId());
        return new MonthlySalesSummary(key, transaction.signedAmount(), transaction.signedQuantity());
    }

    /** 何も計上していない集計。 */
    public static MonthlySalesSummary empty(MonthlySalesKey key) {
        return new MonthlySalesSummary(key, Money.zero(), Quantity.zero());
    }

    public static MonthlySalesSummary of(MonthlySalesKey key, Money totalAmount, Quantity totalQuantity) {
        return new MonthlySalesSummary(key, totalAmount, totalQuantity);
    }

    /** 同じ識別子を持つ集計どうしを足し合わせる。 */
    public MonthlySalesSummary add(MonthlySalesSummary other) {
        if (!key.equals(other.key)) {
            throw new IllegalArgumentException(
                    "識別子が異なる集計は足し合わせられない: " + key + " と " + other.key);
        }
        return new MonthlySalesSummary(key, totalAmount.add(other.totalAmount),
                totalQuantity.add(other.totalQuantity));
    }

    public MonthlySalesKey key() {
        return key;
    }

    public TargetMonth targetMonth() {
        return key.targetMonth();
    }

    public StoreId storeId() {
        return key.storeId();
    }

    public ProductId productId() {
        return key.productId();
    }

    public Money totalAmount() {
        return totalAmount;
    }

    public Quantity totalQuantity() {
        return totalQuantity;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof MonthlySalesSummary that
                && key.equals(that.key)
                && totalAmount.equals(that.totalAmount)
                && totalQuantity.equals(that.totalQuantity);
    }

    @Override
    public int hashCode() {
        return Objects.hash(key, totalAmount, totalQuantity);
    }

    @Override
    public String toString() {
        return "MonthlySalesSummary[key=" + key + ", totalAmount=" + totalAmount
                + ", totalQuantity=" + totalQuantity + "]";
    }
}
