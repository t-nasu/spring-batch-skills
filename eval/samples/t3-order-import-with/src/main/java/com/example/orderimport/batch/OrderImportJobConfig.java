package com.example.orderimport.batch;

import com.example.orderimport.domain.order.InvalidOrderException;
import com.example.orderimport.domain.order.Order;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * orderImportJob の流れ（仕様 4）。ここには処理の順序と異常時の方針だけを書き、業務判断は書かない。
 */
@Configuration
public class OrderImportJobConfig {

    /** 1 回のコミットで扱う注文の件数。最大 300,000 件を 300 回のコミットで取り込む。 */
    private static final int CHUNK_SIZE = 1000;

    /** 読み飛ばせる不正データの上限（仕様 6）。これを超えたら取り込みを失敗させる。 */
    private static final int SKIP_LIMIT = 100;

    @Bean
    public Job orderImportJob(
            JobRepository jobRepository,
            Step importOrderStep,
            Step aggregateDailySalesStep,
            Step notifyFailureStep) {
        return new JobBuilder("orderImportJob", jobRepository)
                .validator(JobArguments.validator())
                .start(importOrderStep)
                    .on(ExitStatus.FAILED.getExitCode()).to(notifyFailureStep)
                .from(importOrderStep)
                    .on("*").to(aggregateDailySalesStep)
                .from(aggregateDailySalesStep)
                    .on(ExitStatus.FAILED.getExitCode()).to(notifyFailureStep)
                .from(aggregateDailySalesStep)
                    .on("*").end()
                .from(notifyFailureStep)
                    .on("*").fail()
                .end()
                .build();
    }

    /**
     * CSV を読み、検証を通った注文を注文表に登録する（仕様 4-1, 4-2）。
     *
     * <p>一時的なエラーの再試行はしない（仕様 6）ため、リトライの設定は置かない。
     */
    @Bean
    public Step importOrderStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Order> orderCsvItemReader,
            ItemProcessor<Order, Order> importOrderItemProcessor,
            ItemWriter<Order> orderItemWriter,
            SkipListener<Order, Order> skippedOrderLoggingListener) {
        return new StepBuilder("importOrderStep", jobRepository)
                .<Order, Order>chunk(CHUNK_SIZE)
                .transactionManager(transactionManager)
                .reader(orderCsvItemReader)
                .processor(importOrderItemProcessor)
                .writer(orderItemWriter)
                .faultTolerant()
                .skip(InvalidOrderException.class, FlatFileParseException.class)
                .skipLimit(SKIP_LIMIT)
                .skipListener(skippedOrderLoggingListener)
                .build();
    }

    /**
     * 業務日付の日次売上を集計して登録する（仕様 4-3）。
     */
    @Bean
    public Step aggregateDailySalesStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet aggregateDailySalesTasklet) {
        return new StepBuilder("aggregateDailySalesStep", jobRepository)
                .tasklet(aggregateDailySalesTasklet, transactionManager)
                .build();
    }

    /**
     * 失敗を運用チームへ知らせる（仕様 4-4）。
     *
     * <p>再開した実行がまた失敗したときにも知らせたいので、完了済みでもやり直す設定にしている。
     */
    @Bean
    public Step notifyFailureStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet notifyFailureTasklet) {
        return new StepBuilder("notifyFailureStep", jobRepository)
                .tasklet(notifyFailureTasklet, transactionManager)
                .allowStartIfComplete(true)
                .build();
    }
}
