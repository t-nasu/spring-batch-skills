package com.example.monthlysales.domain.summary;

import java.util.List;

/** 月次売上集計の保管先。 */
public interface MonthlySalesSummaryRepository {

    /**
     * 集計を加算登録する。同じ識別子の集計がすでにあれば金額と数量を足し込み、無ければ新しく登録する。
     *
     * <p>呼び出し側のトランザクションがロールバックすれば加算も取り消される。
     * これにより、途中で失敗したジョブを再開しても同じ取引が二度計上されることはない。
     */
    void addAll(List<MonthlySalesSummary> summaries);
}
