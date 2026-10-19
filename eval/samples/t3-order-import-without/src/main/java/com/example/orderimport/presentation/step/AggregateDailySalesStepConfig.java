package com.example.orderimport.presentation.step;

import com.example.orderimport.application.sales.DailySalesAggregationService;
import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.presentation.job.JobParameterNames;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 業務日付の日次売上を集計して登録するステップ。
 *
 * <p>取り込みステップが正常終了したときだけ実行される。処理は冪等なので、
 * 再実行しても日次売上が二重計上されることはない。
 */
@Configuration
public class AggregateDailySalesStepConfig {

    public static final String STEP_NAME = "aggregateDailySalesStep";

    @Bean
    public Step aggregateDailySalesStep(JobRepository jobRepository,
                                        PlatformTransactionManager transactionManager,
                                        DailySalesAggregationTasklet dailySalesAggregationTasklet) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(dailySalesAggregationTasklet, transactionManager)
                .build();
    }

    @Bean
    @StepScope
    public DailySalesAggregationTasklet dailySalesAggregationTasklet(
            DailySalesAggregationService dailySalesAggregationService,
            @Value("#{jobParameters['" + JobParameterNames.TARGET_DATE + "']}") String targetDate) {
        return new DailySalesAggregationTasklet(
                dailySalesAggregationService, BusinessDate.parse(targetDate));
    }
}
