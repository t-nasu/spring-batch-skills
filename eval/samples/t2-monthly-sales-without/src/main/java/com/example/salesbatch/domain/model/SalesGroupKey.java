package com.example.salesbatch.domain.model;

import java.util.Comparator;
import java.util.Objects;

/**
 * 集計単位（店舗 × 商品）を表す値オブジェクト。
 *
 * <p>取引明細はこのキー順に読み込まれ、キーが切り替わった時点で 1 グループの集計が確定する。
 * 再開時の位置づけ（どのグループまで登録済みか）にも使う。</p>
 */
public record SalesGroupKey(StoreId storeId, ProductId productId) implements Comparable<SalesGroupKey> {

    private static final Comparator<SalesGroupKey> COMPARATOR =
            Comparator.comparing(SalesGroupKey::storeId).thenComparing(SalesGroupKey::productId);

    public SalesGroupKey {
        Objects.requireNonNull(storeId, "storeId は必須です");
        Objects.requireNonNull(productId, "productId は必須です");
    }

    public static SalesGroupKey of(String storeId, String productId) {
        return new SalesGroupKey(StoreId.of(storeId), ProductId.of(productId));
    }

    @Override
    public int compareTo(SalesGroupKey other) {
        return COMPARATOR.compare(this, other);
    }

    @Override
    public String toString() {
        return "(" + storeId + ", " + productId + ")";
    }
}
