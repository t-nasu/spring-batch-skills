package com.example.salesbatch.application.service;

import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.MonthlySalesSummaryRepository;

import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 月次売上集計の登録・削除を行うアプリケーションサービス。
 */
public class MonthlySalesSummaryRegistrationService {

    private static final Logger log = LoggerFactory.getLogger(MonthlySalesSummaryRegistrationService.class);

    private final MonthlySalesSummaryRepository monthlySalesSummaryRepository;

    public MonthlySalesSummaryRegistrationService(MonthlySalesSummaryRepository monthlySalesSummaryRepository) {
        this.monthlySalesSummaryRepository =
                Objects.requireNonNull(monthlySalesSummaryRepository, "monthlySalesSummaryRepository は必須です");
    }

    /** 集計結果をまとめて登録する。 */
    public void register(List<MonthlySalesSummary> summaries) {
        if (summaries.isEmpty()) {
            return;
        }
        monthlySalesSummaryRepository.saveAll(summaries);
    }

    /**
     * 対象年月の集計結果を削除する。作り直しの前処理として、ジョブ実行の最初に 1 度だけ呼ばれる。
     *
     * @return 削除件数
     */
    public int purge(TargetMonth targetMonth) {
        int deleted = monthlySalesSummaryRepository.deleteByTargetMonth(targetMonth);
        log.info("対象年月の既存の月次売上集計を削除しました: targetMonth={} 削除件数={}",
                targetMonth.asString(), deleted);
        return deleted;
    }
}
