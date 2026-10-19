package com.example.salesbatch.presentation.batch;

import com.example.salesbatch.application.service.MonthlySalesGroupLoaderFactory;
import com.example.salesbatch.application.service.MonthlySalesSummaryRegistrationService;
import com.example.salesbatch.config.MonthlySalesBatchProperties;
import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.presentation.batch.listener.MonthlySalesJobListener;
import com.example.salesbatch.presentation.batch.reader.MonthlySalesSummaryItemReader;
import com.example.salesbatch.presentation.batch.retry.DeadlockRetryPolicy;
import com.example.salesbatch.presentation.batch.tasklet.PurgeMonthlySalesSummaryTasklet;
import com.example.salesbatch.presentation.batch.writer.MonthlySalesSummaryItemWriter;

import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 月次売上集計ジョブの定義。
 *
 * <pre>
 * monthlySalesSummaryJob
 *   ├─ purgeMonthlySalesSummaryStep : 対象年月の既存の集計結果を削除（ジョブインスタンスにつき 1 回）
 *   └─ aggregateMonthlySalesStep    : 取引明細を店舗 × 商品ごとに集計して登録（チャンク処理・再開可能）
 * </pre>
 */
@Configuration(proxyBeanMethods = false)
public class MonthlySalesSummaryJobConfig {

    public static final String JOB_NAME = "monthlySalesSummaryJob";
    public static final String PURGE_STEP_NAME = "purgeMonthlySalesSummaryStep";
    public static final String AGGREGATE_STEP_NAME = "aggregateMonthlySalesStep";

    @Bean
    public Job monthlySalesSummaryJob(
            JobRepository jobRepository,
            MonthlySalesJobListener monthlySalesJobListener,
            Step purgeMonthlySalesSummaryStep,
            Step aggregateMonthlySalesStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(new TargetMonthJobParametersValidator())
                .listener(monthlySalesJobListener)
                .start(purgeMonthlySalesSummaryStep)
                .next(aggregateMonthlySalesStep)
                .build();
    }

    @Bean
    public Step purgeMonthlySalesSummaryStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            PurgeMonthlySalesSummaryTasklet purgeMonthlySalesSummaryTasklet) {
        return new StepBuilder(PURGE_STEP_NAME, jobRepository)
                .tasklet(purgeMonthlySalesSummaryTasklet, transactionManager)
                .build();
    }

    /**
     * 集計ステップ。
     *
     * <p>チャンク（{@code chunk-size} 件の集計単位）ごとにコミットし、
     * 同じトランザクションで進捗（どの集計単位まで登録したか）を保存する。
     * 失敗しても、次の起動では保存された位置から再開する。</p>
     *
     * <p>デッドロックのときだけ、最大 {@code deadlock-retry-count} 回再試行する。</p>
     */
    @Bean
    public Step aggregateMonthlySalesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            MonthlySalesBatchProperties properties,
            MonthlySalesSummaryItemReader monthlySalesSummaryItemReader,
            MonthlySalesSummaryItemWriter monthlySalesSummaryItemWriter) {
        return new StepBuilder(AGGREGATE_STEP_NAME, jobRepository)
                .<MonthlySalesSummary, MonthlySalesSummary>chunk(properties.chunkSize())
                .transactionManager(transactionManager)
                .reader(monthlySalesSummaryItemReader)
                .writer(monthlySalesSummaryItemWriter)
                .faultTolerant()
                .retryPolicy(DeadlockRetryPolicy.create(
                        properties.deadlockRetryCount(),
                        properties.retryInitialInterval(),
                        properties.retryMaxInterval()))
                .build();
    }

    @Bean
    @StepScope
    public MonthlySalesSummaryItemReader monthlySalesSummaryItemReader(
            MonthlySalesGroupLoaderFactory monthlySalesGroupLoaderFactory,
            @Value("#{jobParameters['" + MonthlySalesJobParameters.TARGET_MONTH + "']}") String targetMonth) {
        return new MonthlySalesSummaryItemReader(
                monthlySalesGroupLoaderFactory, TargetMonth.of(targetMonth));
    }

    @Bean
    @StepScope
    public PurgeMonthlySalesSummaryTasklet purgeMonthlySalesSummaryTasklet(
            MonthlySalesSummaryRegistrationService monthlySalesSummaryRegistrationService,
            @Value("#{jobParameters['" + MonthlySalesJobParameters.TARGET_MONTH + "']}") String targetMonth) {
        return new PurgeMonthlySalesSummaryTasklet(
                monthlySalesSummaryRegistrationService, TargetMonth.of(targetMonth));
    }
}
