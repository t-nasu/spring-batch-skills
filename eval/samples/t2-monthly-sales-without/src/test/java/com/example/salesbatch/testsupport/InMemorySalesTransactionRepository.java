package com.example.salesbatch.testsupport;

import com.example.salesbatch.domain.model.RawSalesTransaction;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.SalesTransactionCursor;
import com.example.salesbatch.domain.repository.SalesTransactionRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * テスト用の {@link SalesTransactionRepository}。
 *
 * <p>SQL と同じ並び順（店舗 ID, 商品 ID, 取引 ID）とカーソル条件をメモリ上で再現する。</p>
 */
public class InMemorySalesTransactionRepository implements SalesTransactionRepository {

    private static final Comparator<RawSalesTransaction> ORDER =
            Comparator.comparing(RawSalesTransaction::storeId)
                    .thenComparing(RawSalesTransaction::productId)
                    .thenComparing(RawSalesTransaction::transactionId);

    private final List<RawSalesTransaction> rows = new ArrayList<>();

    private int pageCount;

    public InMemorySalesTransactionRepository add(RawSalesTransaction row) {
        rows.add(row);
        return this;
    }

    public InMemorySalesTransactionRepository addSale(
            long transactionId, String storeId, String productId, String amount, int quantity) {
        return add(row(transactionId, storeId, productId, "SALE", new BigDecimal(amount), quantity));
    }

    public InMemorySalesTransactionRepository addReturn(
            long transactionId, String storeId, String productId, String amount, int quantity) {
        return add(row(transactionId, storeId, productId, "RETURN", new BigDecimal(amount), quantity));
    }

    public InMemorySalesTransactionRepository addInvalid(long transactionId, String storeId, String productId) {
        return add(row(transactionId, storeId, productId, "SALE", null, null));
    }

    private static RawSalesTransaction row(
            long transactionId, String storeId, String productId, String type, BigDecimal amount, Integer quantity) {
        return new RawSalesTransaction(
                transactionId, storeId, productId, LocalDate.of(2025, 9, 15), type, amount, quantity);
    }

    /** SELECT が実行された回数。ページングの挙動の確認に使う。 */
    public int pageCount() {
        return pageCount;
    }

    @Override
    public List<RawSalesTransaction> findPage(
            TargetMonth targetMonth, SalesTransactionCursor cursor, int limit) {
        pageCount++;
        return rows.stream()
                .filter(row -> targetMonth.contains(row.transactionDate()))
                .sorted(ORDER)
                .filter(row -> isAfterCursor(row, cursor))
                .limit(limit)
                .toList();
    }

    private static boolean isAfterCursor(RawSalesTransaction row, SalesTransactionCursor cursor) {
        return switch (cursor) {
            case SalesTransactionCursor.Start ignored -> true;
            case SalesTransactionCursor.AfterGroup afterGroup -> compare(
                    row.storeId(), row.productId(),
                    afterGroup.groupKey().storeId().value(), afterGroup.groupKey().productId().value()) > 0;
            case SalesTransactionCursor.AfterRow afterRow -> {
                int groupComparison =
                        compare(row.storeId(), row.productId(), afterRow.storeId(), afterRow.productId());
                yield groupComparison > 0
                        || (groupComparison == 0 && row.transactionId() > afterRow.transactionId());
            }
        };
    }

    private static int compare(String storeId, String productId, String otherStoreId, String otherProductId) {
        int storeComparison = storeId.compareTo(otherStoreId);
        return storeComparison != 0 ? storeComparison : productId.compareTo(otherProductId);
    }
}
