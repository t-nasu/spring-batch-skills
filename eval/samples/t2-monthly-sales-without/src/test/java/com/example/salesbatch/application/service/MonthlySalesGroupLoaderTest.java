package com.example.salesbatch.application.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.salesbatch.application.exception.SkipLimitExceededException;
import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.Money;
import com.example.salesbatch.domain.model.Quantity;
import com.example.salesbatch.domain.model.SalesGroupKey;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.testsupport.InMemorySalesTransactionRepository;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;

class MonthlySalesGroupLoaderTest {

    private static final TargetMonth TARGET_MONTH = TargetMonth.of("202509");

    private final InMemorySalesTransactionRepository repository = new InMemorySalesTransactionRepository();

    private MonthlySalesGroupLoader loader(int pageSize, int skipLimit) {
        return new MonthlySalesGroupLoader(
                repository, new InvalidTransactionSkipPolicy(skipLimit), TARGET_MONTH, pageSize);
    }

    private static List<MonthlySalesSummary> drain(MonthlySalesGroupLoader loader) {
        List<MonthlySalesSummary> summaries = new ArrayList<>();
        for (Optional<MonthlySalesSummary> next = loader.next(); next.isPresent(); next = loader.next()) {
            summaries.add(next.get());
        }
        return summaries;
    }

    @Test
    void 店舗と商品ごとに集計しページ境界をまたぐグループもまとめる() {
        repository
                .addSale(1L, "S001", "P001", "100", 1)
                .addSale(2L, "S001", "P001", "200", 2)
                .addSale(3L, "S001", "P002", "300", 3)
                .addSale(4L, "S002", "P001", "400", 4)
                .addSale(5L, "S002", "P001", "500", 5);

        // ページサイズ 2 なので S001/P001 と S002/P001 の明細はページ境界をまたぐ
        List<MonthlySalesSummary> summaries = drain(loader(2, 1000));

        assertThat(summaries).hasSize(3);
        assertThat(summaries).extracting(MonthlySalesSummary::groupKey).containsExactly(
                SalesGroupKey.of("S001", "P001"),
                SalesGroupKey.of("S001", "P002"),
                SalesGroupKey.of("S002", "P001"));
        assertThat(summaries.get(0).totalAmount()).isEqualTo(Money.of(new BigDecimal("300")));
        assertThat(summaries.get(0).totalQuantity()).isEqualTo(Quantity.of(3));
        assertThat(summaries.get(0).transactionCount()).isEqualTo(2);
        assertThat(summaries.get(2).totalAmount()).isEqualTo(Money.of(new BigDecimal("900")));
        assertThat(repository.pageCount()).isGreaterThan(1);
    }

    @Test
    void 返品は金額と数量を差し引く() {
        repository
                .addSale(1L, "S001", "P001", "1000", 10)
                .addReturn(2L, "S001", "P001", "250", 3);

        List<MonthlySalesSummary> summaries = drain(loader(10, 1000));

        assertThat(summaries).hasSize(1);
        assertThat(summaries.get(0).totalAmount()).isEqualTo(Money.of(new BigDecimal("750")));
        assertThat(summaries.get(0).totalQuantity()).isEqualTo(Quantity.of(7));
        assertThat(summaries.get(0).transactionCount()).isEqualTo(2);
    }

    @Test
    void 不正データは飛ばして集計を続ける() {
        repository
                .addSale(1L, "S001", "P001", "100", 1)
                .addInvalid(2L, "S001", "P001")
                .addSale(3L, "S001", "P002", "300", 3);

        MonthlySalesGroupLoader loader = loader(10, 1000);
        List<MonthlySalesSummary> summaries = drain(loader);

        assertThat(summaries).hasSize(2);
        assertThat(summaries.get(0).totalAmount()).isEqualTo(Money.of(new BigDecimal("100")));
        assertThat(summaries.get(0).transactionCount()).isEqualTo(1);
        assertThat(loader.committedSkippedCount()).isEqualTo(1);
    }

    @Test
    void グループの明細がすべて不正なら結果を返さない() {
        repository
                .addInvalid(1L, "S001", "P001")
                .addInvalid(2L, "S001", "P001")
                .addSale(3L, "S002", "P001", "300", 3);

        List<MonthlySalesSummary> summaries = drain(loader(10, 1000));

        assertThat(summaries).extracting(MonthlySalesSummary::groupKey)
                .containsExactly(SalesGroupKey.of("S002", "P001"));
    }

    @Test
    void 不正データが上限を超えると例外になる() {
        repository
                .addInvalid(1L, "S001", "P001")
                .addInvalid(2L, "S001", "P002")
                .addInvalid(3L, "S001", "P003");

        MonthlySalesGroupLoader loader = loader(10, 2);

        assertThatThrownBy(() -> drain(loader)).isInstanceOf(SkipLimitExceededException.class);
    }

    @Test
    void 再開すると指定した集計単位より後ろだけを返す() {
        repository
                .addSale(1L, "S001", "P001", "100", 1)
                .addSale(2L, "S001", "P002", "200", 2)
                .addSale(3L, "S002", "P001", "300", 3);

        MonthlySalesGroupLoader loader = loader(10, 1000);
        loader.resumeAfter(SalesGroupKey.of("S001", "P001"), 7);

        List<MonthlySalesSummary> summaries = drain(loader);

        assertThat(summaries).extracting(MonthlySalesSummary::groupKey).containsExactly(
                SalesGroupKey.of("S001", "P002"),
                SalesGroupKey.of("S002", "P001"));
        // 再開時に引き継いだ読み飛ばし件数は維持される
        assertThat(loader.committedSkippedCount()).isEqualTo(7);
    }

    @Test
    void 再開時に読み直す範囲の読み飛ばしは進捗に含めない() {
        repository
                .addSale(1L, "S001", "P001", "100", 1)
                .addInvalid(2L, "S002", "P001")
                .addSale(3L, "S003", "P001", "300", 3);

        MonthlySalesGroupLoader loader = loader(10, 1000);

        // S001/P001 を確定した時点では、その後ろにある S002/P001 の不正データは
        // 再開時に読み直す範囲なので、確定済みの読み飛ばし件数には含めない
        assertThat(loader.next()).isPresent();
        assertThat(loader.lastCompletedGroupKey()).contains(SalesGroupKey.of("S001", "P001"));
        assertThat(loader.committedSkippedCount()).isZero();
        assertThat(loader.skippedCount()).isEqualTo(1);

        // S003/P001 まで確定すれば、その手前の読み飛ばしも確定済みになる
        assertThat(loader.next()).isPresent();
        assertThat(loader.committedSkippedCount()).isEqualTo(1);
    }

    @Test
    void 対象年月の明細が無ければ何も返さない() {
        MonthlySalesGroupLoader loader = loader(10, 1000);

        assertThat(loader.next()).isEmpty();
        assertThat(loader.lastCompletedGroupKey()).isEmpty();
    }

    @Test
    void 対象年月外の明細は読まない() {
        repository.addSale(1L, "S001", "P001", "100", 1);

        MonthlySalesGroupLoader otherMonth = new MonthlySalesGroupLoader(
                repository, new InvalidTransactionSkipPolicy(1000), TargetMonth.of("202508"), 10);

        assertThat(otherMonth.next()).isEmpty();
    }

    @Test
    void 読み込み開始後に再開位置は変更できない() {
        repository.addSale(1L, "S001", "P001", "100", 1);
        MonthlySalesGroupLoader loader = loader(10, 1000);
        loader.next();

        assertThatThrownBy(() -> loader.resumeAfter(SalesGroupKey.of("S001", "P001"), 0))
                .isInstanceOf(IllegalStateException.class);
    }
}
