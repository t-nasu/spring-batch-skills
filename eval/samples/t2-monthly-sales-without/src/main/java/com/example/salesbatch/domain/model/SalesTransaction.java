package com.example.salesbatch.domain.model;

import java.time.LocalDate;
import java.util.Objects;

/**
 * 検証済みの取引明細。
 *
 * <p>このインスタンスが生成できた時点で、集計に使える値であることが保証される
 * （検証は {@link RawSalesTransaction#toSalesTransaction()} が行う）。</p>
 */
public record SalesTransaction(
        TransactionId id,
        SalesGroupKey groupKey,
        TransactionType type,
        Money amount,
        Quantity quantity,
        LocalDate transactionDate) {

    public SalesTransaction {
        Objects.requireNonNull(id, "id は必須です");
        Objects.requireNonNull(groupKey, "groupKey は必須です");
        Objects.requireNonNull(type, "type は必須です");
        Objects.requireNonNull(amount, "amount は必須です");
        Objects.requireNonNull(quantity, "quantity は必須です");
        Objects.requireNonNull(transactionDate, "transactionDate は必須です");
    }

    /** 集計に加算する金額。返品は負値になる。 */
    public Money signedAmount() {
        return amount.multipliedBy(type.sign());
    }

    /** 集計に加算する数量。返品は負値になる。 */
    public Quantity signedQuantity() {
        return quantity.multipliedBy(type.sign());
    }

    public StoreId storeId() {
        return groupKey.storeId();
    }

    public ProductId productId() {
        return groupKey.productId();
    }
}
