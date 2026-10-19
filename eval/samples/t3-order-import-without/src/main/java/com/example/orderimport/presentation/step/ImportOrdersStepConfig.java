package com.example.orderimport.presentation.step;

import com.example.orderimport.application.order.ImportOrderCommand;
import com.example.orderimport.application.order.OrderLineImportService;
import com.example.orderimport.application.order.OrderRegistrationService;
import com.example.orderimport.application.order.OrderValidationException;
import com.example.orderimport.config.OrderImportProperties;
import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.infrastructure.file.OrderCsvFormat;
import com.example.orderimport.infrastructure.file.OrderCsvHeaderValidator;
import com.example.orderimport.infrastructure.file.OrderCsvLineMapper;
import com.example.orderimport.presentation.job.JobParameterNames;
import com.example.orderimport.presentation.listener.SkippedOrderLineListener;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.FlatFileParseException;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * CSV を読み、検証を通った行を注文表に登録するステップ。
 *
 * <p>チャンク指向で構成しており、1 チャンク（既定 1,000 件）が 1 トランザクション
 * になる。読み取り位置はチャンクのコミットとあわせて記録されるため、途中で失敗
 * しても再実行すれば最後のコミット地点から再開する。
 *
 * <p>不正データは例外として扱い、スキップ設定で読み飛ばす。上限（既定 100 件）に
 * 達した時点で Spring Batch がステップを異常終了させる。一時的なエラーの再試行は
 * 仕様により行わないため、リトライは設定していない（既定で無効）。
 */
@Configuration
public class ImportOrdersStepConfig {

    public static final String STEP_NAME = "importOrdersStep";
    private static final String READER_NAME = "orderCsvItemReader";

    @Bean
    public Step importOrdersStep(JobRepository jobRepository,
                                 PlatformTransactionManager transactionManager,
                                 FlatFileItemReader<ImportOrderCommand> orderCsvItemReader,
                                 OrderItemProcessor orderItemProcessor,
                                 OrderItemWriter orderItemWriter,
                                 SkippedOrderLineListener skippedOrderLineListener,
                                 OrderImportProperties properties) {
        return new StepBuilder(STEP_NAME, jobRepository)
                .<ImportOrderCommand, Order>chunk(properties.chunkSize())
                .transactionManager(transactionManager)
                .reader(orderCsvItemReader)
                .processor(orderItemProcessor)
                .writer(orderItemWriter)
                .faultTolerant()
                .skip(
                        // 検証を通らなかった行。
                        OrderValidationException.class,
                        // 列数が合わないなど、行として解釈できなかったもの。
                        FlatFileParseException.class)
                .skipLimit(properties.skipLimit())
                .skipListener(skippedOrderLineListener)
                // リトライは設定しない（仕様により一時的なエラーの再試行は行わない）。
                .build();
    }

    /**
     * 入力 CSV のリーダ。
     *
     * <p>読み取り位置を実行コンテキストに保存するため、ステップスコープで
     * ジョブ引数のファイルパスを受け取る。
     */
    @Bean
    @StepScope
    public FlatFileItemReader<ImportOrderCommand> orderCsvItemReader(
            @Value("#{jobParameters['" + JobParameterNames.INPUT_FILE + "']}") String inputFile,
            OrderImportProperties properties) {
        return new FlatFileItemReaderBuilder<ImportOrderCommand>()
                .name(READER_NAME)
                .resource(new FileSystemResource(inputFile))
                .encoding(properties.encoding())
                .linesToSkip(OrderCsvFormat.HEADER_LINES)
                // 読み飛ばすヘッダ行は、捨てる前に列構成の確認に使う。
                .skippedLinesCallback(new OrderCsvHeaderValidator())
                .lineMapper(new OrderCsvLineMapper())
                // ファイルが無ければ空扱いにせず異常終了させる。
                .strict(true)
                // 再開のために読み取り位置を保存する。再開可能性の要なので明示する。
                .saveState(true)
                .build();
    }

    @Bean
    @StepScope
    public OrderItemProcessor orderItemProcessor(
            OrderLineImportService orderLineImportService,
            @Value("#{jobParameters['" + JobParameterNames.TARGET_DATE + "']}") String targetDate) {
        return new OrderItemProcessor(orderLineImportService, BusinessDate.parse(targetDate));
    }

    @Bean
    public OrderItemWriter orderItemWriter(OrderRegistrationService orderRegistrationService) {
        return new OrderItemWriter(orderRegistrationService);
    }

    @Bean
    public SkippedOrderLineListener skippedOrderLineListener() {
        return new SkippedOrderLineListener();
    }
}
