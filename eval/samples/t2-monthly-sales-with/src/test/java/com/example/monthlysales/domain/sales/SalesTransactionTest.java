package com.example.monthlysales.domain.sales;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.monthlysales.domain.shared.Money;
import com.example.monthlysales.domain.shared.Quantity;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.Test;

class SalesTransactionTest {

    private static final LocalDate DATE = LocalDate.of(2025, 9, 15);

    @Test
    void 売上は金額も数量もそのまま計上する() {
        SalesTransaction transaction = create("T1", "SALE", new BigDecimal("1200.00"), 3L);

        assertThat(transaction.signedAmount()).isEqualTo(Money.of(new BigDecimal("1200.00")));
        assertThat(transaction.signedQuantity()).isEqualTo(Quantity.of(3L));
    }

    @Test
    void 返品は金額も数量も差し引く() {
        SalesTransaction transaction = create("T2", "RETURN", new BigDecimal("1200.00"), 3L);

        assertThat(transaction.signedAmount()).isEqualTo(Money.of(new BigDecimal("-1200.00")));
        assertThat(transaction.signedQuantity()).isEqualTo(Quantity.of(-3L));
    }

    @Test
    void 元の金額と数量は符号を変えない() {
        SalesTransaction transaction = create("T3", "RETURN", new BigDecimal("500.00"), 2L);

        assertThat(transaction.amount()).isEqualTo(Money.of(new BigDecimal("500.00")));
        assertThat(transaction.quantity()).isEqualTo(Quantity.of(2L));
    }

    @Test
    void 未知の区分は取引IDつきで弾く() {
        assertInvalid(() -> create("T4", "CANCEL", BigDecimal.TEN, 1L), "T4");
    }

    @Test
    void 負の金額は弾く() {
        assertInvalid(() -> create("T5", "SALE", new BigDecimal("-1.00"), 1L), "T5");
    }

    @Test
    void 負の数量は弾く() {
        assertInvalid(() -> create("T6", "SALE", BigDecimal.TEN, -1L), "T6");
    }

    @Test
    void 必須の値が無いものは弾く() {
        assertInvalid(() -> SalesTransaction.create("T7", " ", "P1", DATE, "SALE", BigDecimal.TEN, 1L), "T7");
        assertInvalid(() -> SalesTransaction.create("T8", "S1", null, DATE, "SALE", BigDecimal.TEN, 1L), "T8");
        assertInvalid(() -> SalesTransaction.create("T9", "S1", "P1", null, "SALE", BigDecimal.TEN, 1L), "T9");
        assertInvalid(() -> SalesTransaction.create("TA", "S1", "P1", DATE, "SALE", null, 1L), "TA");
        assertInvalid(() -> SalesTransaction.create("TB", "S1", "P1", DATE, "SALE", BigDecimal.TEN, null), "TB");
    }

    @Test
    void 取引IDが読めないものは不明として弾く() {
        assertInvalid(() -> SalesTransaction.create(null, "S1", "P1", DATE, "SALE", BigDecimal.TEN, 1L),
                "(不明)");
    }

    private static void assertInvalid(ThrowingCallable callable, String expectedId) {
        assertThatThrownBy(callable)
                .isInstanceOf(InvalidSalesTransactionException.class)
                .asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories
                        .type(InvalidSalesTransactionException.class))
                .extracting(InvalidSalesTransactionException::transactionId)
                .isEqualTo(expectedId);
    }

    private static SalesTransaction create(String id, String type, BigDecimal amount, Long quantity) {
        return SalesTransaction.create(id, "S1", "P1", DATE, type, amount, quantity);
    }
}
