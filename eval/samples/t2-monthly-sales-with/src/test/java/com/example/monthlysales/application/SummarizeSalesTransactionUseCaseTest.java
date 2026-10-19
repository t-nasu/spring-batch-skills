package com.example.monthlysales.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.monthlysales.domain.sales.InvalidSalesTransactionException;
import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.Money;
import com.example.monthlysales.domain.shared.TargetMonth;
import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class SummarizeSalesTransactionUseCaseTest {

    private static final TargetMonth MONTH = TargetMonth.parse("202509");

    private final SummarizeSalesTransactionUseCase useCase = new SummarizeSalesTransactionUseCase();

    @Test
    void 対象年月の取引はその取引ぶんの集計になる() {
        SalesTransaction transaction = transaction("T1", LocalDate.of(2025, 9, 30));

        MonthlySalesSummary summary = useCase.summarize(MONTH, transaction);

        assertThat(summary.targetMonth()).isEqualTo(MONTH);
        assertThat(summary.totalAmount()).isEqualTo(Money.of(new BigDecimal("100.00")));
    }

    @Test
    void 対象年月の外の取引は飛ばせるよう不正として扱う() {
        SalesTransaction transaction = transaction("T2", LocalDate.of(2025, 10, 1));

        assertThatThrownBy(() -> useCase.summarize(MONTH, transaction))
                .isInstanceOf(InvalidSalesTransactionException.class)
                .hasMessageContaining("T2");
    }

    private static SalesTransaction transaction(String id, LocalDate date) {
        return SalesTransaction.create(id, "S1", "P1", date, "SALE", new BigDecimal("100.00"), 1L);
    }
}
