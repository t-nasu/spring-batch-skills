package com.example.orderimport.presentation.runner;

import com.example.orderimport.application.lock.JobExecutionLockService;
import com.example.orderimport.presentation.job.JobParameterNames;
import com.example.orderimport.presentation.job.OrderImportJobConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.job.parameters.JobParametersValidator;
import org.springframework.batch.core.launch.JobExecutionAlreadyRunningException;
import org.springframework.batch.core.launch.JobInstanceAlreadyCompleteException;
import org.springframework.batch.core.launch.JobOperator;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.stereotype.Component;

/**
 * orderImportJob を起動する。
 *
 * <p>Spring Boot 既定の {@code JobLauncherApplicationRunner} は使わず
 * （{@code spring.batch.job.enabled=false}）、自前のランナーにしている。理由は 2 つ。
 * <ul>
 *   <li>ジョブを開始する前に二重起動ロックを獲得したい。ジョブのリスナーで
 *       ロックを取ると、二重起動のたびに「異常終了したジョブ実行」が記録に残り、
 *       本当の失敗と区別がつかなくなる</li>
 *   <li>失敗の種類（二重起動・引数不正・処理失敗）を終了コードで区別し、
 *       ジョブスケジューラ側で再実行の要否を判断できるようにしたい</li>
 * </ul>
 *
 * <p>ジョブ引数が前回の失敗時と同じであれば、Spring Batch は新しい実行を作らずに
 * 前回の続きから再開する。運用手順は「同じコマンドをもう一度叩く」だけでよい。
 *
 * <p>{@code batch.order-import.runner.enabled=false} を指定すると、このランナーは
 * 登録されない。コンテキストを立ち上げるだけでジョブが走ってほしくないとき
 * （テストや、他のアプリケーションに組み込んで使うとき）に使う。
 */
@Component
@ConditionalOnBooleanProperty(name = "batch.order-import.runner.enabled", matchIfMissing = true)
public class OrderImportJobRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(OrderImportJobRunner.class);

    private final JobOperator jobOperator;
    private final Job orderImportJob;
    private final JobParametersValidator jobParametersValidator;
    private final JobExecutionLockService jobExecutionLockService;

    private volatile JobExitCode exitCode = JobExitCode.SUCCESS;

    public OrderImportJobRunner(JobOperator jobOperator,
                                Job orderImportJob,
                                JobParametersValidator jobParametersValidator,
                                JobExecutionLockService jobExecutionLockService) {
        this.jobOperator = jobOperator;
        this.orderImportJob = orderImportJob;
        this.jobParametersValidator = jobParametersValidator;
        this.jobExecutionLockService = jobExecutionLockService;
    }

    @Override
    public void run(ApplicationArguments args) {
        JobArguments arguments = JobArguments.parse(args);
        log.info("{} を開始します (targetDate={}, inputFile={})",
                OrderImportJobConfig.JOB_NAME, arguments.targetDate(), arguments.inputFile());

        JobParameters parameters = toJobParameters(arguments);
        if (!isValid(parameters)) {
            exitCode = JobExitCode.INVALID_ARGUMENTS;
            return;
        }

        if (!jobExecutionLockService.tryAcquire(OrderImportJobConfig.JOB_NAME)) {
            exitCode = JobExitCode.ALREADY_RUNNING;
            return;
        }

        try {
            exitCode = runJob(arguments, parameters);
        } finally {
            jobExecutionLockService.release(OrderImportJobConfig.JOB_NAME);
        }
    }

    /**
     * ジョブを起動する前に引数を検証する。
     *
     * <p>ジョブに渡してから検証させるのでは遅い。Spring Batch はジョブインスタンスを
     * 作ってから引数を検証するため、引数が不正だと「実行が 1 つもぶら下がっていない
     * ジョブインスタンス」が残ってしまう。すると以降その業務日付で起動するたびに
     * 「再開しようとしたが再開元の実行が無い」という状態になり、打ち間違い 1 回で
     * その日の取り込みが詰む。ここで先に弾いて、不正な引数が記録に残らないようにする。
     *
     * <p>ジョブ側にも同じ検証器を設定してある（別経路で起動された場合の備え）。
     */
    private boolean isValid(JobParameters parameters) {
        try {
            jobParametersValidator.validate(parameters);
            return true;
        } catch (InvalidJobParametersException e) {
            log.error("{}\n{}", e.getMessage(), JobArguments.usage());
            return false;
        }
    }

    private JobExitCode runJob(JobArguments arguments, JobParameters parameters) {
        try {
            JobExecution execution = jobOperator.start(orderImportJob, parameters);
            return execution.getStatus() == BatchStatus.COMPLETED
                    ? JobExitCode.SUCCESS
                    : JobExitCode.JOB_FAILED;

        } catch (InvalidJobParametersException e) {
            // 起動前に検証済みなので通常ここには来ない。念のため終了コードは揃えておく。
            log.error("{}\n{}", e.getMessage(), JobArguments.usage());
            return JobExitCode.INVALID_ARGUMENTS;

        } catch (JobInstanceAlreadyCompleteException e) {
            log.error("業務日付 {} の取り込みは既に正常終了しています。"
                            + "再取り込みが必要な場合は、対象データを整理してから実施してください",
                    arguments.targetDate());
            return JobExitCode.ALREADY_COMPLETED;

        } catch (JobExecutionAlreadyRunningException e) {
            // ロックは獲得できたのにここに来る場合、前回の実行が STARTED のまま
            // 残っている（プロセスが強制終了した）可能性が高い。
            log.error("業務日付 {} のジョブ実行が実行中のまま残っています。"
                            + "BATCH_JOB_EXECUTION の状態を確認し、必要なら停止扱いにしてから再実行してください",
                    arguments.targetDate(), e);
            return JobExitCode.JOB_FAILED;

        } catch (Exception e) {
            log.error("{} の起動に失敗しました", OrderImportJobConfig.JOB_NAME, e);
            return JobExitCode.JOB_FAILED;
        }
    }

    /**
     * ジョブ引数を組み立てる。
     *
     * <p>未指定の引数はあえて詰めずに検証側へ委ねる。「必須項目が足りない」という
     * 報告を 1 か所にまとめるため。
     */
    private JobParameters toJobParameters(JobArguments arguments) {
        JobParametersBuilder builder = new JobParametersBuilder();
        if (arguments.targetDate() != null) {
            builder.addString(JobParameterNames.TARGET_DATE, arguments.targetDate(), true);
        }
        if (arguments.inputFile() != null) {
            // 非識別引数。業務日付が同じなら、パスが変わっても同じジョブインスタンスとして再開する。
            builder.addString(JobParameterNames.INPUT_FILE, arguments.inputFile(), false);
        }
        return builder.toJobParameters();
    }

    @Override
    public int getExitCode() {
        return exitCode.value();
    }
}
