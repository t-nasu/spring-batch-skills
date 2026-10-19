package com.example.salesbatch.domain.repository;

import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.TargetMonth;

import java.util.List;

/**
 * 月次売上集計の永続化ポート。実装はインフラ層（MyBatis）。
 */
public interface MonthlySalesSummaryRepository {

    /**
     * 月次売上集計をまとめて登録する。
     *
     * <p>同一キー（対象年月 × 店舗 × 商品）が既に存在する場合は上書きする。
     * 手動での再実行でも二重登録にならないようにするための冪等な登録。</p>
     */
    void saveAll(List<MonthlySalesSummary> summaries);

    /**
     * 対象年月の月次売上集計を削除する。
     *
     * @return 削除件数
     */
    int deleteByTargetMonth(TargetMonth targetMonth);
}
