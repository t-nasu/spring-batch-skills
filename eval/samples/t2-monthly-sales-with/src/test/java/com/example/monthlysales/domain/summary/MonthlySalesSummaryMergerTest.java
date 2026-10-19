package com.example.monthlysales.domain.summary;

import static com.example.monthlysales.domain.summary.MonthlySalesSummaryTest.transaction;
import static org.assertj.core.api.Assertions.assertThat;

import com.example.monthlysales.domain.shared.Money;
import com.example.monthlysales.domain.shared.Quantity;
import com.example.monthlysales.domain.shared.TargetMonth;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class MonthlySalesSummaryMergerTest {

    private static final TargetMonth MONTH = TargetMonth.parse("202509");

    private final MonthlySalesSummaryMerger merger = new MonthlySalesSummaryMerger();

    @Test
    void 同じ店舗と商品の集計はひとつにまとまる() {
        List<MonthlySalesSummary> merged = merger.merge(List.of(
                summary("T1", "S1", "P1", "SALE", "100.00", 1L),
                summary("T2", "S1", "P2", "SALE", "200.00", 2L),
                summary("T3", "S1", "P1", "SALE", "300.00", 3L),
                summary("T4", "S1", "P1", "RETURN", "50.00", 1L)));

        assertThat(merged).hasSize(2);
        assertThat(merged.get(0).productId().value()).isEqualTo("P1");
        assertThat(merged.get(0).totalAmount()).isEqualTo(Money.of(new BigDecimal("350.00")));
        assertThat(merged.get(0).totalQuantity()).isEqualTo(Quantity.of(3L));
        assertThat(merged.get(1).productId().value()).isEqualTo("P2");
        assertThat(merged.get(1).totalAmount()).isEqualTo(Money.of(new BigDecimal("200.00")));
    }

    @Test
    void 店舗が違えばまとまらない() {
        List<MonthlySalesSummary> merged = merger.merge(List.of(
                summary("T1", "S1", "P1", "SALE", "100.00", 1L),
                summary("T2", "S2", "P1", "SALE", "100.00", 1L)));

        assertThat(merged).hasSize(2);
    }

    @Test
    void 空の入力は空のまま返す() {
        assertThat(merger.merge(List.of())).isEmpty();
    }

    private static MonthlySalesSummary summary(String id, String storeId, String productId,
            String type, String amount, long quantity) {
        return MonthlySalesSummary.of(MONTH,
                transaction(id, storeId, productId, type, amount, quantity));
    }
}
