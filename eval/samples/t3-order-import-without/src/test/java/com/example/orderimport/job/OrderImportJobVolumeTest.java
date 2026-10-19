package com.example.orderimport.job;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderimport.support.JobCounters;
import com.example.orderimport.support.JobTestSupport;
import com.example.orderimport.support.OrderCsvFixture;
import java.math.BigDecimal;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.test.context.TestPropertySource;

/**
 * 本番の設定（チャンク 1,000 件・読み飛ばし上限 100 件）のまま、複数チャンクに
 * またがる量を流す検証。
 *
 * <p>想定件数は通常 5 万件・最大 30 万件だが、それをそのまま流すとテストが遅く
 * なりすぎる。ここでは「チャンク境界をまたいでも件数と金額が合うこと」「複数行
 * INSERT の分割が正しいこと」を確かめられる最小限の量に留めている。実データ量
 * での所要時間の確認は、本番相当の MySQL に対する性能試験で行う。
 */
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:order_import_volume;MODE=MySQL;DB_CLOSE_DELAY=-1")
class OrderImportJobVolumeTest extends JobTestSupport {

    private static final Logger log = LoggerFactory.getLogger(OrderImportJobVolumeTest.class);

    private static final int VALID_ROWS = 5_000;
    private static final int INVALID_ROWS = 50;

    @TempDir
    Path inputDirectory;

    @Test
    @DisplayName("チャンクをまたぐ件数でも、件数・金額・読み飛ばし数がすべて合う")
    void 複数チャンクを正しく処理する() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(VALID_ROWS, "V-")
                .invalidRows(INVALID_ROWS, "X-")
                .writeTo(inputDirectory, "orders_20260301.csv");

        long startedAt = System.currentTimeMillis();
        JobExecution execution = launchJob("20260301", csv);
        long elapsedMillis = System.currentTimeMillis() - startedAt;

        log.info("{} 件（うち不正 {} 件）の取り込みに {} ms かかりました",
                VALID_ROWS + INVALID_ROWS, INVALID_ROWS, elapsedMillis);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(countOrders("20260301")).isEqualTo(VALID_ROWS);
        assertThat(JobCounters.skipCountOf(execution)).isEqualTo(INVALID_ROWS);

        // validRows は 1 件あたり 数量 1 × 単価 100.00。
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_amount FROM daily_sales WHERE business_date = '2026-03-01'", BigDecimal.class))
                .isEqualByComparingTo(BigDecimal.valueOf(VALID_ROWS * 100L));
        assertThat(jdbcTemplate.queryForObject(
                "SELECT order_count FROM daily_sales WHERE business_date = '2026-03-01'", Integer.class))
                .isEqualTo(VALID_ROWS);
    }
}
