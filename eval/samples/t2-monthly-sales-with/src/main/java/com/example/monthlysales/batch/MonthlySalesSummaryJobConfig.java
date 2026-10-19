package com.example.monthlysales.batch;

import com.example.monthlysales.application.NotifyJobFailureUseCase;
import com.example.monthlysales.application.SummarizeSalesTransactionUseCase;
import com.example.monthlysales.domain.sales.InvalidSalesTransactionException;
import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.shared.TargetMonth;
import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemStreamReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * 月次売上集計ジョブの組み立て。
 *
 * <p>ここには処理の流れだけを書き、業務の判断は application と domain に置く。
 */
@Configuration
@EnableConfigurationProperties(MonthlySalesJobProperties.class)
public class MonthlySalesSummaryJobConfig {

    public static final String JOB_NAME = "monthlySalesSummaryJob";
    public static final String AGGREGATE_STEP_NAME = "aggregateMonthlySalesStep";
    public static final String NOTIFY_FAILURE_STEP_NAME = "notifyFailureStep";

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final MonthlySalesJobProperties properties;

    public MonthlySalesSummaryJobConfig(JobRepository jobRepository,
            PlatformTransactionManager transactionManager, MonthlySalesJobProperties properties) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.properties = properties;
    }

    /**
     * 集計ステップが失敗したときだけ通知ステップへ進み、ジョブ全体を失敗で終える。
     *
     * <p>対象年月は識別用のジョブパラメータなので、同じ年月で完了済みのジョブは
     * Spring Batch が拒否する。これで二重起動を防ぐ。
     */
    @Bean
    public Job monthlySalesSummaryJob(Step aggregateMonthlySalesStep, Step notifyFailureStep) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(new TargetMonthJobParametersValidator())
                .start(aggregateMonthlySalesStep)
                    .on("FAILED").to(notifyFailureStep).on("*").fail()
                .from(aggregateMonthlySalesStep)
                    .on("*").end()
                .end()
                .build();
    }

    /**
     * 取引明細を一定件数ずつ読み、集計へ足し込むステップ。
     *
     * <p>不正データは上限まで飛ばして続け、DB のデッドロックだけはまとまりごとに試行し直す。
     * 読み出し順が決まっているため、途中で失敗しても中断した位置から再開できる。
     * この再開は 1 スレッドで動かすことが前提なので、並列化してはならない。
     */
    @Bean
    public Step aggregateMonthlySalesStep(
            ItemStreamReader<SalesTransaction> salesTransactionItemReader,
            ItemProcessor<SalesTransaction, MonthlySalesSummary> salesTransactionItemProcessor,
            ItemWriter<MonthlySalesSummary> monthlySalesSummaryItemWriter) {
        return new StepBuilder(AGGREGATE_STEP_NAME, jobRepository)
                .<SalesTransaction, MonthlySalesSummary>chunk(properties.chunkSize())
                .transactionManager(transactionManager)
                .reader(salesTransactionItemReader)
                .processor(salesTransactionItemProcessor)
                .writer(monthlySalesSummaryItemWriter)
                .faultTolerant()
                .skip(InvalidSalesTransactionException.class)
                .skipLimit(properties.skipLimit())
                .skipListener(new SkippedSalesTransactionLogger())
                // MySQL のデッドロックは SQLState 40001 として返り、Spring はこの型に翻訳する
                .retry(CannotAcquireLockException.class)
                .retryLimit(properties.deadlockRetryLimit())
                .build();
    }

    @Bean
    @StepScope
    public SalesTransactionItemProcessor salesTransactionItemProcessor(
            SummarizeSalesTransactionUseCase useCase,
            @Value("#{jobParameters['targetMonth']}") String targetMonth) {
        return new SalesTransactionItemProcessor(useCase, TargetMonth.parse(targetMonth));
    }

    @Bean
    public Step notifyFailureStep(NotifyJobFailureUseCase notifyJobFailureUseCase) {
        return new StepBuilder(NOTIFY_FAILURE_STEP_NAME, jobRepository)
                .tasklet(new NotifyFailureTasklet(notifyJobFailureUseCase), transactionManager)
                .build();
    }
}
