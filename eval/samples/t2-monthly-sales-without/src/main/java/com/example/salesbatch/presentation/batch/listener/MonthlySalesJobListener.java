package com.example.salesbatch.presentation.batch.listener;

import com.example.salesbatch.application.port.BatchFailureReport;
import com.example.salesbatch.application.port.OperationNotifier;
import com.example.salesbatch.config.MonthlySalesBatchProperties;
import com.example.salesbatch.presentation.batch.MonthlySalesJobParameters;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

/**
 * ジョブの開始・終了のログ出力、異常終了時の運用チームへの通知、終了期限の監視を行うリスナー。
 */
@Component
public class MonthlySalesJobListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(MonthlySalesJobListener.class);

    /** 通知に載せる原因例外の段数。 */
    private static final int MAX_CAUSE_DEPTH = 5;

    private final OperationNotifier operationNotifier;
    private final MonthlySalesBatchProperties properties;

    public MonthlySalesJobListener(
            OperationNotifier operationNotifier, MonthlySalesBatchProperties properties) {
        this.operationNotifier = Objects.requireNonNull(operationNotifier, "operationNotifier は必須です");
        this.properties = Objects.requireNonNull(properties, "properties は必須です");
    }

    @Override
    public void beforeJob(JobExecution jobExecution) {
        log.info("ジョブを開始します: job={} targetMonth={} jobExecutionId={}",
                jobName(jobExecution), targetMonth(jobExecution), jobExecution.getId());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            log.info("ステップが終了しました: step={} status={} read={} write={} commit={} rollback={}",
                    stepExecution.getStepName(),
                    stepExecution.getStatus(),
                    stepExecution.getReadCount(),
                    stepExecution.getWriteCount(),
                    stepExecution.getCommitCount(),
                    stepExecution.getRollbackCount());
        }

        Duration elapsed = elapsed(jobExecution);
        log.info("ジョブが終了しました: job={} targetMonth={} status={} 所要時間={}",
                jobName(jobExecution), targetMonth(jobExecution), jobExecution.getStatus(), format(elapsed));

        warnIfDeadlineExceeded(jobExecution);

        if (jobExecution.getStatus() == BatchStatus.FAILED) {
            operationNotifier.notifyFailure(toFailureReport(jobExecution));
        }
    }

    private void warnIfDeadlineExceeded(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        if (startTime == null || endTime == null) {
            return;
        }
        LocalDateTime deadlineAt = startTime.toLocalDate().atTime(properties.deadline());
        if (!deadlineAt.isAfter(startTime)) {
            // 期限の時刻を過ぎてから開始した場合は翌日の同時刻を期限とみなす
            deadlineAt = deadlineAt.plusDays(1);
        }
        if (endTime.isAfter(deadlineAt)) {
            log.warn("終了期限を超過しました: 期限={} 終了={} 超過={}",
                    deadlineAt, endTime, format(Duration.between(deadlineAt, endTime)));
        }
    }

    private BatchFailureReport toFailureReport(JobExecution jobExecution) {
        return new BatchFailureReport(
                jobName(jobExecution),
                targetMonth(jobExecution),
                jobExecution.getId(),
                Objects.requireNonNullElseGet(jobExecution.getEndTime(), LocalDateTime::now),
                failureMessages(jobExecution));
    }

    /**
     * 失敗の内容を集める。
     *
     * <p>ステップの中で起きた例外はステップ実行に記録され、ジョブ実行には積まれないため、
     * 両方から集める。</p>
     */
    private static List<String> failureMessages(JobExecution jobExecution) {
        List<String> messages = new ArrayList<>();
        jobExecution.getFailureExceptions().forEach(failure -> messages.add(describe(failure)));
        for (StepExecution stepExecution : jobExecution.getStepExecutions()) {
            if (stepExecution.getStatus() != BatchStatus.FAILED) {
                continue;
            }
            stepExecution.getFailureExceptions().forEach(failure ->
                    messages.add("step=" + stepExecution.getStepName() + " " + describe(failure)));
        }
        return messages;
    }

    /** 例外とその原因を、通知に載せられる 1 行にまとめる。 */
    private static String describe(Throwable throwable) {
        StringBuilder causeChain = new StringBuilder();
        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (depth > 0) {
                causeChain.append(" <- ");
            }
            causeChain.append(current.getClass().getSimpleName()).append(": ").append(current.getMessage());
            current = current.getCause() == current ? null : current.getCause();
        }
        return causeChain.toString();
    }

    private static String jobName(JobExecution jobExecution) {
        return jobExecution.getJobInstance() == null ? "unknown" : jobExecution.getJobInstance().getJobName();
    }

    private static String targetMonth(JobExecution jobExecution) {
        return jobExecution.getJobParameters().getString(MonthlySalesJobParameters.TARGET_MONTH);
    }

    private static Duration elapsed(JobExecution jobExecution) {
        LocalDateTime startTime = jobExecution.getStartTime();
        LocalDateTime endTime = jobExecution.getEndTime();
        if (startTime == null || endTime == null) {
            return Duration.ZERO;
        }
        return Duration.between(startTime, endTime);
    }

    private static String format(Duration duration) {
        return "%d:%02d:%02d".formatted(duration.toHours(), duration.toMinutesPart(), duration.toSecondsPart());
    }
}
