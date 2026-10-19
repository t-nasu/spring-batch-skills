package com.example.orderimport.presentation.step;

import com.example.orderimport.application.sales.DailySalesAggregationService;
import com.example.orderimport.application.sales.DailySalesAggregationService.AggregationResult;
import com.example.orderimport.domain.model.BusinessDate;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * 日次売上を集計するステップの処理本体。
 *
 * <p>集計は SQL 1 文で完結し、結果も商品単位に畳まれた少数の行にしかならないため、
 * チャンク処理ではなく 1 回で終わる Tasklet として実装している。
 */
public class DailySalesAggregationTasklet implements Tasklet {

    private final DailySalesAggregationService dailySalesAggregationService;
    private final BusinessDate businessDate;

    public DailySalesAggregationTasklet(DailySalesAggregationService dailySalesAggregationService,
                                        BusinessDate businessDate) {
        this.dailySalesAggregationService = dailySalesAggregationService;
        this.businessDate = businessDate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        AggregationResult result = dailySalesAggregationService.aggregate(businessDate);

        // 実行履歴から結果を追えるよう、登録件数をステップの書き込み件数として残す。
        contribution.incrementWriteCount(result.registeredRows());
        return RepeatStatus.FINISHED;
    }
}
