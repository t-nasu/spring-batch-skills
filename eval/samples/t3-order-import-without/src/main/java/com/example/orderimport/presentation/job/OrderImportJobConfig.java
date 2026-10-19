package com.example.orderimport.presentation.job;

import com.example.orderimport.presentation.listener.JobFailureNotificationListener;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * orderImportJob の組み立て。
 *
 * <p>処理の流れは次の 2 ステップ。
 * <ol>
 *   <li>{@code importOrdersStep}: CSV を読み、検証を通った行を注文表に登録する</li>
 *   <li>{@code aggregateDailySalesStep}: 業務日付の日次売上を集計して登録する</li>
 * </ol>
 *
 * <p>{@code next} は直前のステップが COMPLETED のときだけ次へ進むため、「取り込みが
 * 成功したら集計する」という仕様がそのまま表現されている。取り込みが異常終了した
 * 場合、集計は実行されない。
 *
 * <p>再開可能性はジョブ単位で有効（{@code preventRestart} を呼ばない既定の挙動）。
 * 同じ業務日付で再実行すると、完了済みのステップは読み飛ばされ、失敗したステップは
 * 最後にコミットされたチャンクの続きから再開する。
 */
@Configuration
public class OrderImportJobConfig {

    public static final String JOB_NAME = "orderImportJob";

    @Bean
    public Job orderImportJob(JobRepository jobRepository,
                              @Qualifier("importOrdersStep") Step importOrdersStep,
                              @Qualifier("aggregateDailySalesStep") Step aggregateDailySalesStep,
                              OrderImportJobParametersValidator parametersValidator,
                              JobFailureNotificationListener failureNotificationListener) {
        return new JobBuilder(JOB_NAME, jobRepository)
                .validator(parametersValidator)
                .listener(failureNotificationListener)
                .start(importOrdersStep)
                .next(aggregateDailySalesStep)
                .build();
    }
}
