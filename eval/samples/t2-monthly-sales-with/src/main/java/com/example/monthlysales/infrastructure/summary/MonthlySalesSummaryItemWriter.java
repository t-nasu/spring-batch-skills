package com.example.monthlysales.infrastructure.summary;

import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import com.example.monthlysales.domain.summary.MonthlySalesSummaryMerger;
import com.example.monthlysales.domain.summary.MonthlySalesSummaryRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/**
 * 一定件数ぶんの集計をまとめて書き込む Writer。
 *
 * <p>同じ店舗・商品の集計は DB へ渡す前にドメインの決まりで畳み込み、書き込み回数を減らす。
 */
public class MonthlySalesSummaryItemWriter implements ItemWriter<MonthlySalesSummary> {

    private final MonthlySalesSummaryRepository repository;
    private final MonthlySalesSummaryMerger merger;

    public MonthlySalesSummaryItemWriter(MonthlySalesSummaryRepository repository,
            MonthlySalesSummaryMerger merger) {
        this.repository = repository;
        this.merger = merger;
    }

    @Override
    public void write(Chunk<? extends MonthlySalesSummary> chunk) {
        if (chunk.isEmpty()) {
            return;
        }
        List<MonthlySalesSummary> summaries = new ArrayList<>(chunk.getItems());
        repository.addAll(merger.merge(summaries));
    }
}
