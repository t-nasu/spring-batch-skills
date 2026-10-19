package com.example.monthlysales.domain.summary;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.Money;
import com.example.monthlysales.domain.shared.ProductId;
import com.example.monthlysales.domain.shared.Quantity;
import com.example.monthlysales.domain.shared.StoreId;
import com.example.monthlysales.domain.shared.TargetMonth;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class MonthlySalesSummaryTest {

    private static final TargetMonth MONTH = TargetMonth.parse("202509");
    private static final LocalDate DATE = LocalDate.of(2025, 9, 15);

    @Test
    void 売上と返品を足し合わせると返品ぶんが差し引かれる() {
        MonthlySalesSummary sale = MonthlySalesSummary.of(MONTH,
                transaction("T1", "S1", "P1", "SALE", "1000.00", 5L));
        MonthlySalesSummary aReturn = MonthlySalesSummary.of(MONTH,
                transaction("T2", "S1", "P1", "RETURN", "300.00", 2L));

        MonthlySalesSummary merged = sale.add(aReturn);

        assertThat(merged.totalAmount()).isEqualTo(Money.of(new BigDecimal("700.00")));
        assertThat(merged.totalQuantity()).isEqualTo(Quantity.of(3L));
    }

    @Test
    void 返品が売上を上回ると合計は負になる() {
        MonthlySalesSummary sale = MonthlySalesSummary.of(MONTH,
                transaction("T1", "S1", "P1", "SALE", "100.00", 1L));
        MonthlySalesSummary aReturn = MonthlySalesSummary.of(MONTH,
                transaction("T2", "S1", "P1", "RETURN", "500.00", 4L));

        MonthlySalesSummary merged = sale.add(aReturn);

        assertThat(merged.totalAmount()).isEqualTo(Money.of(new BigDecimal("-400.00")));
        assertThat(merged.totalQuantity()).isEqualTo(Quantity.of(-3L));
    }

    @Test
    void 識別子は対象年月と店舗と商品の組である() {
        MonthlySalesSummary summary = MonthlySalesSummary.of(MONTH,
                transaction("T1", "S1", "P1", "SALE", "100.00", 1L));

        assertThat(summary.key()).isEqualTo(
                new MonthlySalesKey(MONTH, StoreId.of("S1"), ProductId.of("P1")));
    }

    @Test
    void 店舗が違う集計は足し合わせられない() {
        MonthlySalesSummary s1 = MonthlySalesSummary.of(MONTH,
                transaction("T1", "S1", "P1", "SALE", "100.00", 1L));
        MonthlySalesSummary s2 = MonthlySalesSummary.of(MONTH,
                transaction("T2", "S2", "P1", "SALE", "100.00", 1L));

        assertThatThrownBy(() -> s1.add(s2)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void 商品が違う集計は足し合わせられない() {
        MonthlySalesSummary s1 = MonthlySalesSummary.of(MONTH,
                transaction("T1", "S1", "P1", "SALE", "100.00", 1L));
        MonthlySalesSummary s2 = MonthlySalesSummary.of(MONTH,
                transaction("T2", "S1", "P2", "SALE", "100.00", 1L));

        assertThatThrownBy(() -> s1.add(s2)).isInstanceOf(IllegalArgumentException.class);
    }

    static SalesTransaction transaction(String id, String storeId, String productId, String type,
            String amount, long quantity) {
        return SalesTransaction.create(id, storeId, productId, DATE, type,
                new BigDecimal(amount), quantity);
    }
}
