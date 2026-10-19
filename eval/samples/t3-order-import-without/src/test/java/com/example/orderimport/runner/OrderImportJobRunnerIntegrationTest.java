package com.example.orderimport.runner;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderimport.application.lock.JobExecutionLockService;
import com.example.orderimport.application.port.JobLockStore;
import com.example.orderimport.presentation.job.OrderImportJobConfig;
import com.example.orderimport.presentation.job.OrderImportJobParametersValidator;
import com.example.orderimport.presentation.runner.JobExitCode;
import com.example.orderimport.presentation.runner.OrderImportJobRunner;
import com.example.orderimport.support.JobTestSupport;
import java.nio.file.Path;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.DefaultApplicationArguments;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import com.example.orderimport.support.OrderCsvFixture;

/**
 * 起動処理の検証。
 *
 * <p>ランナーはテストプロファイルでは Bean 登録されないため（コンテキスト起動と
 * 同時にジョブが走らないようにするため）、ここでは同じ依存を渡して手で組み立て、
 * コマンドラインから叩いたときと同じ経路を通す。
 */
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:order_import_runner;MODE=MySQL;DB_CLOSE_DELAY=-1")
class OrderImportJobRunnerIntegrationTest extends JobTestSupport {

    @TempDir
    Path inputDirectory;

    @Autowired
    JobOperator autowiredJobOperator;

    @Autowired
    Job autowiredOrderImportJob;

    @Autowired
    JobExecutionLockService lockService;

    @Autowired
    JobLockStore lockStore;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    OrderImportJobParametersValidator parametersValidator;

    private OrderImportJobRunner runner;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM batch_job_lock");
        notificationGateway.clear();
        runner = new OrderImportJobRunner(
                autowiredJobOperator, autowiredOrderImportJob, parametersValidator, lockService);
    }

    private int runWith(String... args) {
        runner.run(new DefaultApplicationArguments(args));
        return runner.getExitCode();
    }

    @Test
    @DisplayName("正常終了なら終了コード 0、ロックは解放されている")
    void 正常終了する() {
        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(3, "RUN-")
                .writeTo(inputDirectory, "orders_20260201.csv");

        int exitCode = runWith("--targetDate=20260201", "--inputFile=" + csv);

        assertThat(exitCode).isEqualTo(JobExitCode.SUCCESS.value());
        assertThat(countOrders("20260201")).isEqualTo(3);
        assertThat(lockStore.find(OrderImportJobConfig.JOB_NAME))
                .as("終了時にロックを解放すること")
                .isEmpty();
    }

    @Test
    @DisplayName("--name=value 形式でも name=value 形式でも受け取る")
    void 引数の書き方を問わない() {
        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(1, "ARG-")
                .writeTo(inputDirectory, "orders_20260202.csv");

        int exitCode = runWith("targetDate=20260202", "inputFile=" + csv);

        assertThat(exitCode).isEqualTo(JobExitCode.SUCCESS.value());
        assertThat(countOrders("20260202")).isEqualTo(1);
    }

    @Test
    @DisplayName("既に実行中なら、何もせず終了コード 2 を返す")
    void 二重起動を終了コードで知らせる() {
        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(3, "DUP-")
                .writeTo(inputDirectory, "orders_20260203.csv");

        // 別プロセスが実行中の状況を作る。
        assertThat(lockStore.acquire(OrderImportJobConfig.JOB_NAME, "other-host:1234", LocalDateTime.now()))
                .isTrue();

        int exitCode = runWith("--targetDate=20260203", "--inputFile=" + csv);

        assertThat(exitCode).isEqualTo(JobExitCode.ALREADY_RUNNING.value());
        assertThat(countOrders("20260203")).as("1 件も取り込まないこと").isZero();
        assertThat(notificationGateway.sent())
                .as("二重起動は取り込みの失敗ではないので通知しない")
                .isEmpty();
        assertThat(lockStore.find(OrderImportJobConfig.JOB_NAME))
                .as("他プロセスのロックを消してしまわないこと")
                .isPresent();
    }

    @Test
    @DisplayName("引数が足りなければ終了コード 3、ジョブは動かさない")
    void 引数不足を終了コードで知らせる() {
        int exitCode = runWith("--targetDate=20260204");

        assertThat(exitCode).isEqualTo(JobExitCode.INVALID_ARGUMENTS.value());
        assertThat(lockStore.find(OrderImportJobConfig.JOB_NAME)).isEmpty();
        assertThat(notificationGateway.sent()).isEmpty();
    }

    @Test
    @DisplayName("入力ファイルが無ければ終了コード 3、ジョブは動かさない")
    void ファイルが無ければ起動しない() {
        int exitCode = runWith("--targetDate=20260207",
                "--inputFile=" + inputDirectory.resolve("missing.csv"));

        assertThat(exitCode).isEqualTo(JobExitCode.INVALID_ARGUMENTS.value());
        assertThat(lockStore.find(OrderImportJobConfig.JOB_NAME)).isEmpty();
    }

    @Test
    @DisplayName("引数を打ち間違えても、その業務日付が以後ずっと起動できなくなったりしない")
    void 引数の打ち間違いが尾を引かない() {
        // Spring Batch は引数を検証する前にジョブインスタンスを作ってしまうため、
        // 不正な引数のまま起動させると「実行がぶら下がっていないインスタンス」が
        // 残り、以降その業務日付での起動が通らなくなる。起動前に弾くことで防いでいる。
        assertThat(runWith("--targetDate=20260208",
                "--inputFile=" + inputDirectory.resolve("typo.csv")))
                .isEqualTo(JobExitCode.INVALID_ARGUMENTS.value());

        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(2, "FIX-")
                .writeTo(inputDirectory, "orders_20260208.csv");

        assertThat(runWith("--targetDate=20260208", "--inputFile=" + csv))
                .as("パスを直して叩き直せば、そのまま取り込めること")
                .isEqualTo(JobExitCode.SUCCESS.value());
        assertThat(countOrders("20260208")).isEqualTo(2);
    }

    @Test
    @DisplayName("取り込みに失敗したら終了コード 1 で、運用チームへ通知済み")
    void 失敗を終了コードで知らせる() {
        Path csv = OrderCsvFixture.withStandardHeader()
                .invalidRows(101, "NG-")
                .writeTo(inputDirectory, "orders_20260205.csv");

        int exitCode = runWith("--targetDate=20260205", "--inputFile=" + csv);

        assertThat(exitCode).isEqualTo(JobExitCode.JOB_FAILED.value());
        assertThat(notificationGateway.sent()).hasSize(1);
        assertThat(lockStore.find(OrderImportJobConfig.JOB_NAME))
                .as("失敗しても次の実行のためにロックは解放する")
                .isEmpty();
    }

    @Test
    @DisplayName("完了済みの業務日付を再実行したら終了コード 4 で止める")
    void 完了済みの再実行を止める() {
        Path csv = OrderCsvFixture.withStandardHeader()
                .validRows(2, "DONE-")
                .writeTo(inputDirectory, "orders_20260206.csv");

        assertThat(runWith("--targetDate=20260206", "--inputFile=" + csv))
                .isEqualTo(JobExitCode.SUCCESS.value());

        int exitCode = runWith("--targetDate=20260206", "--inputFile=" + csv);

        assertThat(exitCode).isEqualTo(JobExitCode.ALREADY_COMPLETED.value());
        assertThat(countOrders("20260206")).as("二重に取り込まれないこと").isEqualTo(2);
    }
}
