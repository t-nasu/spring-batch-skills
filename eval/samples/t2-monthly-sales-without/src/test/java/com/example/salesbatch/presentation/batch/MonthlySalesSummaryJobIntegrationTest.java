package com.example.salesbatch.presentation.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.salesbatch.infrastructure.repository.MyBatisMonthlySalesSummaryRepository;
import com.example.salesbatch.testsupport.FailableMonthlySalesSummaryRepository;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * 月次売上集計ジョブの結合テスト（H2 の MySQL 互換モード）。
 */
@SpringBootTest
@ActiveProfiles("test")
class MonthlySalesSummaryJobIntegrationTest {

    private static final String TARGET_MONTH = "202509";

    @Autowired
    private JobOperator jobOperator;

    @Autowired
    private Job monthlySalesSummaryJob;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FailableMonthlySalesSummaryRepository failableRepository;

    @TestConfiguration(proxyBeanMethods = false)
    static class FailureInjectionConfiguration {

        /** 書き込みを失敗させられるようにするため、本来のリポジトリを包んで差し替える。 */
        @Bean
        @Primary
        FailableMonthlySalesSummaryRepository failableMonthlySalesSummaryRepository(
                MyBatisMonthlySalesSummaryRepository delegate) {
            return new FailableMonthlySalesSummaryRepository(delegate);
        }
    }

    @BeforeEach
    void setUp() {
        failableRepository.reset();
        jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION_CONTEXT");
        jdbcTemplate.execute("DELETE FROM BATCH_STEP_EXECUTION");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_CONTEXT");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION_PARAMS");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_EXECUTION");
        jdbcTemplate.execute("DELETE FROM BATCH_JOB_INSTANCE");
        jdbcTemplate.execute("DELETE FROM sales_transaction");
        jdbcTemplate.execute("DELETE FROM monthly_sales_summary");
    }

    @Test
    @DisplayName("店舗・商品ごとに集計し、返品は差し引いて登録する")
    void 集計して登録する() throws Exception {
        givenStandardTransactions();

        JobExecution jobExecution = launch(TARGET_MONTH);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(summaries()).containsExactly(
                summaryRow("S001", "P001", "1200.00", 2, 3),
                summaryRow("S001", "P002", "200.00", 1, 1),
                summaryRow("S002", "P001", "0.00", 0, 2),
                summaryRow("S002", "P002", "10.00", 1, 1),
                summaryRow("S003", "P001", "999.99", 3, 1));
    }

    @Test
    @DisplayName("対象年月外の取引明細は集計しない")
    void 対象年月だけを集計する() throws Exception {
        givenStandardTransactions();

        launch("202508");

        assertThat(summaries()).containsExactly(summaryRow("202508", "S001", "P001", "77.00", 1, 1));
    }

    @Test
    @DisplayName("対象年月の既存データは作り直す")
    void 既存の集計結果を作り直す() throws Exception {
        givenStandardTransactions();
        jdbcTemplate.update("""
                INSERT INTO monthly_sales_summary
                  (target_month, store_id, product_id, total_amount, total_quantity,
                   transaction_count, created_at, updated_at)
                VALUES (?, 'S999', 'P999', 1, 1, 1, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
                """, TARGET_MONTH);

        launch(TARGET_MONTH);

        assertThat(summaries()).extracting(row -> row.get("store_id")).doesNotContain("S999");
        assertThat(summaries()).hasSize(5);
    }

    @Test
    @DisplayName("不正データは読み飛ばして集計を続ける")
    void 不正データを読み飛ばす() throws Exception {
        givenStandardTransactions();
        // 金額なし・数量 0・未知の取引区分
        insertTransaction(901L, "S001", "P001", "2025-09-20", "SALE", null, 1);
        insertTransaction(902L, "S001", "P001", "2025-09-21", "SALE", new BigDecimal("100"), 0);
        insertTransaction(903L, "S003", "P001", "2025-09-22", "REFUND", new BigDecimal("100"), 1);

        JobExecution jobExecution = launch(TARGET_MONTH);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // 読み飛ばした明細は集計に含まれない
        assertThat(summaries()).containsExactly(
                summaryRow("S001", "P001", "1200.00", 2, 3),
                summaryRow("S001", "P002", "200.00", 1, 1),
                summaryRow("S002", "P001", "0.00", 0, 2),
                summaryRow("S002", "P002", "10.00", 1, 1),
                summaryRow("S003", "P001", "999.99", 3, 1));
    }

    @Test
    @DisplayName("不正データが上限を超えたら異常終了する")
    void 不正データの上限を超えたら失敗する() throws Exception {
        givenStandardTransactions();
        // skip-limit = 3（application-test.yml）に対して 4 件
        insertTransaction(901L, "S001", "P001", "2025-09-20", "SALE", null, 1);
        insertTransaction(902L, "S001", "P002", "2025-09-20", "SALE", null, 1);
        insertTransaction(903L, "S002", "P001", "2025-09-20", "SALE", null, 1);
        insertTransaction(904L, "S003", "P001", "2025-09-20", "SALE", null, 1);

        JobExecution jobExecution = launch(TARGET_MONTH);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.FAILED);
        assertThat(jobExecution.getStepExecutions())
                .filteredOn(step -> step.getStepName().equals(MonthlySalesSummaryJobConfig.AGGREGATE_STEP_NAME))
                .allSatisfy(step -> {
                    assertThat(step.getStatus()).isEqualTo(BatchStatus.FAILED);
                    assertThat(step.getFailureExceptions())
                            .anySatisfy(failure -> assertThat(rootCauseMessage(failure))
                                    .contains("不正データの読み飛ばしが上限を超えました"));
                });
    }

    @Test
    @DisplayName("途中で失敗しても、同じ対象年月で再実行すると続きから再開する")
    void 失敗した箇所から再開する() throws Exception {
        givenStandardTransactions();
        // チャンクサイズ 2・集計単位 5 件なので、2 回目の書き込み（3・4 件目）で失敗させる
        failableRepository.failOnSaveCall(2, new IllegalStateException("書き込みに失敗しました（テスト）"));

        JobExecution failed = launch(TARGET_MONTH);

        assertThat(failed.getStatus()).isEqualTo(BatchStatus.FAILED);
        // 1 チャンク目はコミット済み
        assertThat(summaries()).containsExactly(
                summaryRow("S001", "P001", "1200.00", 2, 3),
                summaryRow("S001", "P002", "200.00", 1, 1));

        // デッドロック以外の例外は再試行しない（書き込みは 2 回呼ばれただけ）
        assertThat(failableRepository.saveCount()).isEqualTo(2);

        failableRepository.reset();
        JobExecution restarted = launch(TARGET_MONTH);

        assertThat(restarted.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        assertThat(restarted.getId()).isNotEqualTo(failed.getId());

        // 削除ステップは完了済みなので再実行されない（登録済みの集計結果が消えない）
        assertThat(restarted.getStepExecutions()).extracting(StepExecution::getStepName)
                .containsExactly(MonthlySalesSummaryJobConfig.AGGREGATE_STEP_NAME);

        // 残り 3 件の集計単位だけを読み直している（最初からではない）
        StepExecution aggregateStep = restarted.getStepExecutions().iterator().next();
        assertThat(aggregateStep.getReadCount()).isEqualTo(3);
        assertThat(aggregateStep.getWriteCount()).isEqualTo(3);

        assertThat(summaries()).containsExactly(
                summaryRow("S001", "P001", "1200.00", 2, 3),
                summaryRow("S001", "P002", "200.00", 1, 1),
                summaryRow("S002", "P001", "0.00", 0, 2),
                summaryRow("S002", "P002", "10.00", 1, 1),
                summaryRow("S003", "P001", "999.99", 3, 1));
    }

    @Test
    @DisplayName("デッドロックが起きたら再試行して処理を続ける")
    void デッドロックは再試行する() throws Exception {
        givenStandardTransactions();
        failableRepository.failOnSaveCall(1, new CannotAcquireLockException(
                "could not write", new SQLException("Deadlock found when trying to get lock", "40001", 1213)));

        JobExecution jobExecution = launch(TARGET_MONTH);

        assertThat(jobExecution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
        // 1 回目の書き込みが失敗し、再試行で成功しているので 1 回分多く呼ばれる
        assertThat(failableRepository.saveCount()).isEqualTo(4);
        assertThat(summaries()).hasSize(5);
    }

    @Test
    @DisplayName("同じ対象年月で正常終了したジョブは二度実行できない")
    void 同一対象年月の二重起動を防ぐ() throws Exception {
        givenStandardTransactions();
        assertThat(launch(TARGET_MONTH).getStatus()).isEqualTo(BatchStatus.COMPLETED);

        assertThatThrownBy(() -> launch(TARGET_MONTH))
                .isInstanceOf(JobInstanceAlreadyCompleteException.class);
    }

    @Test
    @DisplayName("対象年月の指定が無い・不正なら起動できない")
    void 起動引数を検証する() {
        assertThatThrownBy(() -> launch(new JobParameters()))
                .hasMessageContaining("targetMonth");
        assertThatThrownBy(() -> launch("2025-09"))
                .hasMessageContaining("yyyyMM");
    }

    // -----------------------------------------------------------------------
    // テストデータ・ヘルパー
    // -----------------------------------------------------------------------

    private JobExecution launch(String targetMonth) throws Exception {
        return launch(new JobParametersBuilder()
                .addString(MonthlySalesJobParameters.TARGET_MONTH, targetMonth)
                .toJobParameters());
    }

    private JobExecution launch(JobParameters parameters) throws Exception {
        return jobOperator.start(monthlySalesSummaryJob, parameters);
    }

    /** 5 つの集計単位になる取引明細（対象年月外のものも含む）。 */
    private void givenStandardTransactions() {
        insertTransaction(1L, "S001", "P001", "2025-09-01", "SALE", new BigDecimal("1000.00"), 2);
        insertTransaction(2L, "S001", "P001", "2025-09-15", "SALE", new BigDecimal("500.00"), 1);
        insertTransaction(3L, "S001", "P001", "2025-09-30", "RETURN", new BigDecimal("300.00"), 1);
        insertTransaction(4L, "S001", "P002", "2025-09-10", "SALE", new BigDecimal("200.00"), 1);
        insertTransaction(5L, "S002", "P001", "2025-09-11", "SALE", new BigDecimal("50.00"), 5);
        insertTransaction(6L, "S002", "P001", "2025-09-12", "RETURN", new BigDecimal("50.00"), 5);
        insertTransaction(7L, "S002", "P002", "2025-09-13", "SALE", new BigDecimal("10.00"), 1);
        insertTransaction(8L, "S003", "P001", "2025-09-14", "SALE", new BigDecimal("999.99"), 3);

        // 対象年月外（前月末日・翌月初日）
        insertTransaction(101L, "S001", "P001", "2025-08-31", "SALE", new BigDecimal("77.00"), 1);
        insertTransaction(102L, "S001", "P001", "2025-10-01", "SALE", new BigDecimal("88.00"), 1);
    }

    private void insertTransaction(
            long transactionId, String storeId, String productId, String transactionDate,
            String type, BigDecimal amount, Integer quantity) {
        jdbcTemplate.update("""
                INSERT INTO sales_transaction
                  (transaction_id, store_id, product_id, transaction_date, type, amount, quantity)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """, transactionId, storeId, productId, LocalDate.parse(transactionDate), type, amount, quantity);
    }

    private List<Map<String, Object>> summaries() {
        return jdbcTemplate.queryForList("""
                SELECT target_month, store_id, product_id, total_amount, total_quantity, transaction_count
                FROM monthly_sales_summary
                ORDER BY target_month, store_id, product_id
                """);
    }

    private static Map<String, Object> summaryRow(
            String storeId, String productId, String totalAmount, int totalQuantity, int transactionCount) {
        return summaryRow(TARGET_MONTH, storeId, productId, totalAmount, totalQuantity, transactionCount);
    }

    private static Map<String, Object> summaryRow(
            String targetMonth, String storeId, String productId, String totalAmount,
            int totalQuantity, int transactionCount) {
        return Map.of(
                "target_month", targetMonth,
                "store_id", storeId,
                "product_id", productId,
                "total_amount", new BigDecimal(totalAmount),
                "total_quantity", totalQuantity,
                "transaction_count", transactionCount);
    }

    private static String rootCauseMessage(Throwable throwable) {
        Throwable current = throwable;
        StringBuilder messages = new StringBuilder();
        while (current != null) {
            messages.append(current.getMessage()).append(" / ");
            current = current.getCause() == current ? null : current.getCause();
        }
        return messages.toString();
    }
}
