package com.example.monthlysales.infrastructure.summary;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.Money;
import com.example.monthlysales.domain.shared.TargetMonth;
import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import com.example.monthlysales.domain.summary.MonthlySalesSummaryMerger;
import com.example.monthlysales.domain.summary.MonthlySalesSummaryRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.Chunk;

class MonthlySalesSummaryItemWriterTest {

    private static final TargetMonth MONTH = TargetMonth.parse("202509");
    private static final LocalDate DATE = LocalDate.of(2025, 9, 15);

    private final RecordingRepository repository = new RecordingRepository();
    private final MonthlySalesSummaryItemWriter writer =
            new MonthlySalesSummaryItemWriter(repository, new MonthlySalesSummaryMerger());

    @Test
    void まとまりの中で同じ店舗と商品を畳み込んでから渡す() throws Exception {
        writer.write(new Chunk<>(List.of(
                summary("T1", "S1", "P1", "SALE", "100.00"),
                summary("T2", "S1", "P1", "SALE", "200.00"),
                summary("T3", "S2", "P1", "SALE", "300.00"))));

        assertThat(repository.saved).hasSize(1);
        List<MonthlySalesSummary> passed = repository.saved.get(0);
        assertThat(passed).hasSize(2);
        assertThat(passed.get(0).totalAmount()).isEqualTo(Money.of(new BigDecimal("300.00")));
        assertThat(passed.get(1).totalAmount()).isEqualTo(Money.of(new BigDecimal("300.00")));
    }

    @Test
    void 空のまとまりではDBを呼ばない() throws Exception {
        writer.write(new Chunk<>(List.of()));

        assertThat(repository.saved).isEmpty();
    }

    private static MonthlySalesSummary summary(String id, String storeId, String productId,
            String type, String amount) {
        SalesTransaction transaction = SalesTransaction.create(id, storeId, productId, DATE, type,
                new BigDecimal(amount), 1L);
        return MonthlySalesSummary.of(MONTH, transaction);
    }

    private static final class RecordingRepository implements MonthlySalesSummaryRepository {

        private final List<List<MonthlySalesSummary>> saved = new ArrayList<>();

        @Override
        public void addAll(List<MonthlySalesSummary> summaries) {
            saved.add(List.copyOf(summaries));
        }
    }
}
