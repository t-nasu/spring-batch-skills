package com.example.orderimport.job;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.orderimport.config.OrderImportProperties;
import com.example.orderimport.support.JobCounters;
import com.example.orderimport.support.JobTestSupport;
import com.example.orderimport.support.OrderCsvFixture;
import java.math.BigDecimal;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

/**
 * orderImportJob を端から端まで動かす検証。
 */
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:order_import_job;MODE=MySQL;DB_CLOSE_DELAY=-1")
class OrderImportJobIntegrationTest extends JobTestSupport {

    @TempDir
    Path inputDirectory;

    @Autowired
    OrderImportProperties properties;

    @BeforeEach
    void clearNotifications() {
        notificationGateway.clear();
    }

    @Test
    @DisplayName("設定上の読み飛ばし上限は仕様どおり 100 件")
    void 読み飛ばし上限が仕様どおり() {
        assertThat(properties.skipLimit()).isEqualTo(100);
    }

    @Test
    @DisplayName("正常系: 注文を登録し、続けて日次売上を商品ごとに集計する")
    void 取り込みと集計が通る() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .row("ORD-0001", "C001", "P001", "3", "1000.00", "2026-01-18 10:00:00")
                .row("ORD-0002", "C002", "P001", "2", "1000.00", "2026-01-18 11:00:00")
                .row("ORD-0003", "C001", "P002", "1", "150.50", "2026-01-18 12:00:00")
                .writeTo(inputDirectory, "orders_20260118.csv");

        JobExecution execution = launchJob("20260118", csv);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(countOrders("20260118")).isEqualTo(3);

        // 日次売上は「業務日付 × 商品」の粒度。
        List<Map<String, Object>> sales = jdbcTemplate.queryForList("""
                SELECT product_code, order_count, total_quantity, total_amount
                FROM daily_sales WHERE business_date = '2026-01-18' ORDER BY product_code
                """);
        assertThat(sales).hasSize(2);

        assertThat(sales.get(0)).containsEntry("PRODUCT_CODE", "P001");
        assertThat((Number) sales.get(0).get("ORDER_COUNT")).hasToString("2");
        assertThat((Number) sales.get(0).get("TOTAL_QUANTITY")).hasToString("5");
        assertThat((BigDecimal) sales.get(0).get("TOTAL_AMOUNT")).isEqualByComparingTo("5000.00");

        assertThat(sales.get(1)).containsEntry("PRODUCT_CODE", "P002");
        assertThat((BigDecimal) sales.get(1).get("TOTAL_AMOUNT")).isEqualByComparingTo("150.50");

        assertThat(notificationGateway.sent()).isEmpty();
    }

    @Test
    @DisplayName("注文日時が前日でも、集計対象は業務日付で決まる")
    void 集計対象は業務日付で決まる() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .row("ORD-0101", "C001", "P001", "1", "100.00", "2026-01-18 23:59:59")
                .row("ORD-0102", "C001", "P001", "1", "100.00", "2026-01-17 00:00:01")
                .writeTo(inputDirectory, "orders_20260119.csv");

        JobExecution execution = launchJob("20260119", csv);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // 注文日時が 2 日にまたがっていても、両方とも業務日付 20260119 に集計される。
        assertThat(countOrders("20260119")).isEqualTo(2);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT total_quantity FROM daily_sales WHERE business_date = '2026-01-19'", Long.class))
                .isEqualTo(2L);
    }

    @Test
    @DisplayName("不正データは飛ばして続け、正常な行だけ登録する")
    void 不正データを飛ばして続ける() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .row("ORD-0201", "C001", "P001", "2", "100.00", "2026-01-18 10:00:00")
                // 顧客がマスタに無い
                .row("ORD-0202", "C999", "P001", "1", "100.00", "2026-01-18 10:00:00")
                // 商品がマスタに無い
                .row("ORD-0203", "C001", "P999", "1", "100.00", "2026-01-18 10:00:00")
                // 数量が 1 未満
                .row("ORD-0204", "C001", "P001", "0", "100.00", "2026-01-18 10:00:00")
                // 列数が足りない
                .rawLine("ORD-0205,C001,P001")
                .row("ORD-0206", "C001", "P001", "3", "100.00", "2026-01-18 10:00:00")
                .writeTo(inputDirectory, "orders_20260120.csv");

        JobExecution execution = launchJob("20260120", csv);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(countOrders("20260120")).isEqualTo(2);
        assertThat(JobCounters.skipCountOf(execution)).isEqualTo(4);

        // 正常終了なので通知は飛ばない（読み飛ばしの明細はログに残る）。
        assertThat(notificationGateway.sent()).isEmpty();
    }

    @Test
    @DisplayName("読み飛ばしがちょうど上限 100 件なら、最後まで取り込む")
    void 上限ちょうどまでは続行する() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .invalidRows(100, "NG-")
                .validRows(5, "OK-")
                .writeTo(inputDirectory, "orders_20260121.csv");

        JobExecution execution = launchJob("20260121", csv);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(JobCounters.skipCountOf(execution)).isEqualTo(100);
        assertThat(countOrders("20260121")).isEqualTo(5);
        assertThat(notificationGateway.sent()).isEmpty();
    }

    @Test
    @DisplayName("読み飛ばしが上限を超えたら異常終了し、運用チームへ通知する")
    void 上限超過で異常終了する() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .invalidRows(101, "NG-")
                .validRows(5, "OK-")
                .writeTo(inputDirectory, "orders_20260122.csv");

        JobExecution execution = launchJob("20260122", csv);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        // 集計ステップは実行されない。
        assertThat(countDailySales("20260122")).isZero();

        assertThat(notificationGateway.sent()).hasSize(1);
        assertThat(notificationGateway.sent().get(0).subject()).contains("異常終了", "20260122");
        assertThat(notificationGateway.sent().get(0).body()).contains("importOrdersStep");
    }

    @Test
    @DisplayName("ヘッダの列順が変わっていたら、1 行も取り込まずに異常終了する")
    void ヘッダ不一致で異常終了する() throws Exception {
        Path csv = OrderCsvFixture.withHeader("注文番号,商品コード,顧客コード,数量,単価,注文日時")
                .row("ORD-0301", "P001", "C001", "1", "100.00", "2026-01-18 10:00:00")
                .writeTo(inputDirectory, "orders_20260123.csv");

        JobExecution execution = launchJob("20260123", csv);

        assertThat(execution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(countOrders("20260123")).isZero();
        assertThat(notificationGateway.sent()).hasSize(1);
        assertThat(notificationGateway.sent().get(0).body()).contains("ヘッダ行が想定と異なります");
    }

    @Test
    @DisplayName("入力ファイルが無ければ、ジョブ実行を作る前に弾く")
    void ファイルが無ければ起動しない() {
        Path missing = inputDirectory.resolve("orders_20260124.csv");

        assertThatThrownBy(() -> launchJob("20260124", missing))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining("入力ファイルが存在しません");

        // 起動前に弾くので、失敗したジョブ実行の履歴は残らない。
        assertThat(notificationGateway.sent()).isEmpty();
    }

    @Test
    @DisplayName("業務日付の書式が不正なら弾く")
    void 業務日付が不正なら起動しない() throws Exception {
        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(1, "OK-")
                .writeTo(inputDirectory, "orders.csv");

        assertThatThrownBy(() -> launchJob("2026-01-25", csv))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining("yyyyMMdd");
    }

    @Test
    @DisplayName("引数が足りなければ、不足している分をまとめて報告する")
    void 引数不足をまとめて報告する() {
        assertThatThrownBy(() ->
                jobOperator.start(orderImportJob, new JobParametersBuilder().toJobParameters()))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining("targetDate は必須です")
                .hasMessageContaining("inputFile は必須です");
    }
}
