package com.example.salesbatch.domain.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.salesbatch.domain.exception.InvalidSalesTransactionException;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class RawSalesTransactionTest {

    private static final LocalDate DATE = LocalDate.of(2025, 9, 10);

    private static RawSalesTransaction raw(
            String storeId, String productId, String type, BigDecimal amount, Integer quantity) {
        return new RawSalesTransaction(1L, storeId, productId, DATE, type, amount, quantity);
    }

    @Test
    void 正しい明細は検証を通る() {
        SalesTransaction transaction =
                raw("S001", "P001", "SALE", new BigDecimal("1200.50"), 3).toSalesTransaction();

        assertThat(transaction.id()).isEqualTo(TransactionId.of(1L));
        assertThat(transaction.groupKey()).isEqualTo(SalesGroupKey.of("S001", "P001"));
        assertThat(transaction.type()).isEqualTo(TransactionType.SALE);
        assertThat(transaction.amount()).isEqualTo(Money.of(new BigDecimal("1200.50")));
        assertThat(transaction.quantity()).isEqualTo(Quantity.of(3));
    }

    @Test
    void 取引区分は小文字でも解釈する() {
        assertThat(raw("S001", "P001", "return", BigDecimal.TEN, 1).toSalesTransaction().type())
                .isEqualTo(TransactionType.RETURN);
    }

    @Test
    void 返品は集計時に金額と数量を差し引く() {
        SalesTransaction transaction = raw("S001", "P001", "RETURN", new BigDecimal("500"), 2).toSalesTransaction();

        assertThat(transaction.signedAmount()).isEqualTo(Money.of(new BigDecimal("-500")));
        assertThat(transaction.signedQuantity()).isEqualTo(Quantity.of(-2));
    }

    @ParameterizedTest(name = "[{index}] {5}")
    @CsvSource(nullValues = "null", value = {
            "null, P001, SALE,   100, 1, 店舗 ID が null",
            "'',   P001, SALE,   100, 1, 店舗 ID が空",
            "S001, null, SALE,   100, 1, 商品 ID が null",
            "S001, '  ', SALE,   100, 1, 商品 ID が空白のみ",
            "S001, P001, null,   100, 1, 取引区分が null",
            "S001, P001, REFUND, 100, 1, 取引区分が未知の値",
            "S001, P001, SALE,   null, 1, 金額が null",
            "S001, P001, SALE,   -1,  1, 金額が負数",
            "S001, P001, SALE,   100, null, 数量が null",
            "S001, P001, SALE,   100, 0, 数量が 0",
            "S001, P001, SALE,   100, -2, 数量が負数",
    })
    void 不正な明細は取引IDつきの例外になる(
            String storeId, String productId, String type, BigDecimal amount, Integer quantity, String caseName) {
        assertThatThrownBy(() -> raw(storeId, productId, type, amount, quantity).toSalesTransaction())
                .isInstanceOf(InvalidSalesTransactionException.class)
                .satisfies(thrown ->
                        assertThat(((InvalidSalesTransactionException) thrown).transactionId()).isEqualTo(1L))
                .as(caseName);
    }

    @Test
    void 取引日が未設定なら不正データ() {
        RawSalesTransaction target =
                new RawSalesTransaction(7L, "S001", "P001", null, "SALE", BigDecimal.ONE, 1);

        assertThatThrownBy(target::toSalesTransaction)
                .isInstanceOf(InvalidSalesTransactionException.class)
                .hasMessageContaining("取引日");
    }
}
