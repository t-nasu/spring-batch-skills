package com.example.salesbatch.domain.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.Money;
import com.example.salesbatch.domain.model.Quantity;
import com.example.salesbatch.domain.model.RawSalesTransaction;
import com.example.salesbatch.domain.model.SalesGroupKey;
import com.example.salesbatch.domain.model.SalesTransaction;
import com.example.salesbatch.domain.model.TargetMonth;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class MonthlySalesAggregationTest {

    private static final TargetMonth TARGET_MONTH = TargetMonth.of("202509");
    private static final SalesGroupKey GROUP_KEY = SalesGroupKey.of("S001", "P001");

    private static SalesTransaction transaction(String storeId, String productId, String type, String amount, int quantity) {
        return new RawSalesTransaction(
                1L, storeId, productId, LocalDate.of(2025, 9, 1), type, new BigDecimal(amount), quantity)
                .toSalesTransaction();
    }

    @Test
    void 売上を加算し返品を減算する() {
        MonthlySalesAggregation aggregation = new MonthlySalesAggregation(TARGET_MONTH, GROUP_KEY);

        aggregation.add(transaction("S001", "P001", "SALE", "1000.00", 2));
        aggregation.add(transaction("S001", "P001", "SALE", "500.50", 1));
        aggregation.add(transaction("S001", "P001", "RETURN", "300.50", 1));

        MonthlySalesSummary summary = aggregation.toSummary();

        assertThat(summary.targetMonth()).isEqualTo(TARGET_MONTH);
        assertThat(summary.groupKey()).isEqualTo(GROUP_KEY);
        assertThat(summary.totalAmount()).isEqualTo(Money.of(new BigDecimal("1200.00")));
        assertThat(summary.totalQuantity()).isEqualTo(Quantity.of(2));
        assertThat(summary.transactionCount()).isEqualTo(3);
    }

    @Test
    void 返品が売上を上回ると合計は負になる() {
        MonthlySalesAggregation aggregation = new MonthlySalesAggregation(TARGET_MONTH, GROUP_KEY);

        aggregation.add(transaction("S001", "P001", "SALE", "100", 1));
        aggregation.add(transaction("S001", "P001", "RETURN", "250", 3));

        MonthlySalesSummary summary = aggregation.toSummary();

        assertThat(summary.totalAmount()).isEqualTo(Money.of(new BigDecimal("-150")));
        assertThat(summary.totalQuantity()).isEqualTo(Quantity.of(-2));
    }

    @Test
    void 明細が無ければ合計はゼロ() {
        MonthlySalesAggregation aggregation = new MonthlySalesAggregation(TARGET_MONTH, GROUP_KEY);

        assertThat(aggregation.isEmpty()).isTrue();
        assertThat(aggregation.toSummary().totalAmount()).isEqualTo(Money.ZERO);
        assertThat(aggregation.toSummary().transactionCount()).isZero();
    }

    @Test
    void 集計単位が違う明細は受け付けない() {
        MonthlySalesAggregation aggregation = new MonthlySalesAggregation(TARGET_MONTH, GROUP_KEY);

        assertThatThrownBy(() -> aggregation.add(transaction("S002", "P001", "SALE", "100", 1)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("集計単位が一致しません");
    }
}
