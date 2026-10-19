package com.example.orderimport.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderimport.application.order.OrderRegistrationService;
import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.presentation.step.OrderItemWriter;
import com.example.orderimport.support.JobTestSupport;
import com.example.orderimport.support.OrderCsvFixture;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.TestPropertySource;

/**
 * 「失敗した所から再開したい」という要件の検証。
 *
 * <p>登録の途中で一度だけ落ちる書き込み処理に差し替え、同じ引数で動かし直したときに
 * <ul>
 *   <li>コミット済みのチャンクが失われないこと</li>
 *   <li>既に登録した注文を二重に登録しないこと</li>
 *   <li>続きから再開して最後まで通ること</li>
 * </ul>
 * を確かめる。チャンクサイズを 2 に絞り、どこで落ちたかを明確にしている。
 */
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:order_import_restart;MODE=MySQL;DB_CLOSE_DELAY=-1",
        "batch.order-import.chunk-size=2"
})
@Import(OrderImportJobRestartIntegrationTest.FailOnceWriterConfig.class)
class OrderImportJobRestartIntegrationTest extends JobTestSupport {

    private static final String TARGET_DATE = "20260118";

    @TempDir
    Path inputDirectory;

    @Test
    @DisplayName("途中で落ちても、同じ引数で動かし直せば続きから再開して完走する")
    void 失敗した所から再開する() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(6, "ORD-")
                .writeTo(inputDirectory, "orders_20260118.csv");

        // 1 回目: 3 番目のチャンク（ORD-0005, ORD-0006）の登録で落ちる。
        JobExecution first = launchJob(TARGET_DATE, csv);

        assertThat(first.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(countOrders(TARGET_DATE))
                .as("コミット済みの 2 チャンク分は残る")
                .isEqualTo(4);
        assertThat(countDailySales(TARGET_DATE))
                .as("取り込みが失敗したので集計は行われない")
                .isZero();
        assertThat(notificationGateway.sent())
                .as("異常終了したので運用チームへ通知される")
                .hasSize(1);

        // 2 回目: 同じ引数で再実行する。これが運用上の「再開」手順。
        JobExecution second = launchJob(TARGET_DATE, csv);

        assertThat(second.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(second.getJobInstanceId())
                .as("新しいジョブインスタンスではなく、同じインスタンスの再開であること")
                .isEqualTo(first.getJobInstanceId());

        assertThat(countOrders(TARGET_DATE))
                .as("残り 2 件が足されて 6 件。再読み込みによる重複は起きない")
                .isEqualTo(6);

        List<String> orderNumbers = jdbcTemplate.queryForList(
                "SELECT order_number FROM orders ORDER BY order_number", String.class);
        assertThat(orderNumbers).containsExactly(
                "ORD-0001", "ORD-0002", "ORD-0003", "ORD-0004", "ORD-0005", "ORD-0006");

        // 再開後に集計まで進み、6 件分が過不足なく積まれている。
        assertThat(countDailySales(TARGET_DATE)).isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_amount FROM daily_sales WHERE business_date = '2026-01-18'", BigDecimal.class))
                .isEqualByComparingTo("600.00");
    }

    /**
     * 登録を一度だけ失敗させる書き込み処理。
     *
     * <p>2 回目以降は素通りするので、再実行がそのまま「再開」の検証になる。
     */
    @TestConfiguration
    static class FailOnceWriterConfig {

        @Bean
        @Primary
        OrderItemWriter failOnceOrderItemWriter(OrderRegistrationService orderRegistrationService) {
            return new FailOnceOrderItemWriter(orderRegistrationService, "ORD-0005");
        }
    }

    private static class FailOnceOrderItemWriter extends OrderItemWriter {

        private final String failOnOrderNumber;
        private boolean alreadyFailed;

        FailOnceOrderItemWriter(OrderRegistrationService orderRegistrationService, String failOnOrderNumber) {
            super(orderRegistrationService);
            this.failOnOrderNumber = failOnOrderNumber;
        }

        @Override
        public void write(Chunk<? extends Order> chunk) {
            boolean containsTrigger = chunk.getItems().stream()
                    .anyMatch(order -> order.orderNumber().value().equals(failOnOrderNumber));
            if (!alreadyFailed && containsTrigger) {
                alreadyFailed = true;
                throw new IllegalStateException("テスト用に仕込んだ登録失敗: " + failOnOrderNumber);
            }
            super.write(chunk);
        }
    }
}
