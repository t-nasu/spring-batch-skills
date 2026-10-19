package com.example.monthlysales.domain.summary;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 同じ識別子を持つ月次売上集計をひとつに畳み込むドメインサービス。
 *
 * <p>一定件数ずつ取引を読む作りのため、同じ店舗・商品の集計が 1 回の書き込みに
 * 複数現れる。DB へ渡す前にここでまとめ、書き込み回数を減らす。
 */
public class MonthlySalesSummaryMerger {

    /**
     * 識別子ごとに 1 件へまとめる。並び順は識別子が最初に現れた順を保つ。
     */
    public List<MonthlySalesSummary> merge(List<MonthlySalesSummary> summaries) {
        Map<MonthlySalesKey, MonthlySalesSummary> merged = new LinkedHashMap<>();
        for (MonthlySalesSummary summary : summaries) {
            merged.merge(summary.key(), summary, MonthlySalesSummary::add);
        }
        return List.copyOf(merged.values());
    }
}
