package com.example.monthlysales.domain.sales;

import com.example.monthlysales.domain.shared.Money;
import com.example.monthlysales.domain.shared.ProductId;
import com.example.monthlysales.domain.shared.Quantity;
import com.example.monthlysales.domain.shared.StoreId;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Objects;

/**
 * 取引明細。
 *
 * <p>生成できた時点で業務上の決まりを満たしていることを保証する。
 * 決まりを満たさない値は {@link InvalidSalesTransactionException} で弾き、
 * 呼び出し側（バッチ）がその取引を飛ばせるようにする。
 */
public final class SalesTransaction {

    private final SalesTransactionId id;
    private final StoreId storeId;
    private final ProductId productId;
    private final LocalDate transactionDate;
    private final TransactionType type;
    private final Money amount;
    private final Quantity quantity;

    private SalesTransaction(SalesTransactionId id, StoreId storeId, ProductId productId,
            LocalDate transactionDate, TransactionType type, Money amount, Quantity quantity) {
        this.id = id;
        this.storeId = storeId;
        this.productId = productId;
        this.transactionDate = transactionDate;
        this.type = type;
        this.amount = amount;
        this.quantity = quantity;
    }

    /**
     * DB から読んだ素の値から取引明細を組み立てる。
     *
     * <p>不正なものは次のとおりで、いずれも {@link InvalidSalesTransactionException} になる。
     * 取引 ID・店舗 ID・商品 ID が空、取引日が無い、区分が {@code SALE} / {@code RETURN} 以外、
     * 金額または数量が無いか負。
     */
    public static SalesTransaction create(String transactionId, String storeId, String productId,
            LocalDate transactionDate, String typeCode, BigDecimal amount, Long quantity) {

        requirePresent(transactionId, transactionId, "取引 ID が無い");
        requirePresent(storeId, transactionId, "店舗 ID が無い");
        requirePresent(productId, transactionId, "商品 ID が無い");
        if (transactionDate == null) {
            throw new InvalidSalesTransactionException(transactionId, "取引日が無い");
        }

        TransactionType type = TransactionType.from(typeCode)
                .orElseThrow(() -> new InvalidSalesTransactionException(transactionId,
                        "区分が SALE でも RETURN でもない: " + typeCode));

        if (amount == null) {
            throw new InvalidSalesTransactionException(transactionId, "金額が無い");
        }
        Money money = Money.of(amount);
        if (money.isNegative()) {
            throw new InvalidSalesTransactionException(transactionId, "金額が負である: " + money);
        }

        if (quantity == null) {
            throw new InvalidSalesTransactionException(transactionId, "数量が無い");
        }
        Quantity count = Quantity.of(quantity);
        if (count.isNegative()) {
            throw new InvalidSalesTransactionException(transactionId, "数量が負である: " + count);
        }

        return new SalesTransaction(SalesTransactionId.of(transactionId), StoreId.of(storeId),
                ProductId.of(productId), transactionDate, type, money, count);
    }

    private static void requirePresent(String value, String transactionId, String reason) {
        if (value == null || value.isBlank()) {
            throw new InvalidSalesTransactionException(transactionId, reason);
        }
    }

    /** 集計に計上する金額。返品は符号を反転する。 */
    public Money signedAmount() {
        return type.subtracts() ? amount.negate() : amount;
    }

    /** 集計に計上する数量。返品は符号を反転する。 */
    public Quantity signedQuantity() {
        return type.subtracts() ? quantity.negate() : quantity;
    }

    public SalesTransactionId id() {
        return id;
    }

    public StoreId storeId() {
        return storeId;
    }

    public ProductId productId() {
        return productId;
    }

    public LocalDate transactionDate() {
        return transactionDate;
    }

    public TransactionType type() {
        return type;
    }

    public Money amount() {
        return amount;
    }

    public Quantity quantity() {
        return quantity;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof SalesTransaction that && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "SalesTransaction[id=" + id + ", storeId=" + storeId + ", productId=" + productId
                + ", transactionDate=" + transactionDate + ", type=" + type
                + ", amount=" + amount + ", quantity=" + quantity + "]";
    }
}
