package com.example.orderimport.presentation.listener;

import com.example.orderimport.application.notification.JobFailureNotificationService;
import com.example.orderimport.application.notification.JobFailureReport;
import com.example.orderimport.presentation.job.JobParameterNames;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.listener.JobExecutionListener;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.stereotype.Component;

/**
 * ジョブの終了を監視し、正常終了でなければ運用チームへ通知する。
 *
 * <p>ジョブ単位のリスナーにしているのは、取り込みステップと集計ステップのどちらで
 * 失敗しても、またスキップ上限超過で失敗しても、通知経路を 1 本に保つため。
 *
 * <p>二重起動の検知や引数不正は、ジョブを開始する前に
 * {@link com.example.orderimport.presentation.runner.OrderImportJobRunner} が弾くため
 * ここには届かない。これらは「取り込み・集計の失敗」ではなく起動手順の問題であり、
 * 仕様上の通知対象に含めていない（ログと終了コードで判別できる）。
 */
@Component
public class JobFailureNotificationListener implements JobExecutionListener {

    private static final Logger log = LoggerFactory.getLogger(JobFailureNotificationListener.class);

    /** 通知に載せる原因チェーンの深さ。これ以上は本文が読みにくくなるので打ち切る。 */
    private static final int MAX_CAUSE_DEPTH = 5;

    private final JobFailureNotificationService jobFailureNotificationService;

    public JobFailureNotificationListener(JobFailureNotificationService jobFailureNotificationService) {
        this.jobFailureNotificationService = jobFailureNotificationService;
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
            log.info("{} が正常終了しました: {}",
                    jobExecution.getJobInstance().getJobName(), summarize(jobExecution));
            return;
        }
        log.error("{} が異常終了しました: {}",
                jobExecution.getJobInstance().getJobName(), summarize(jobExecution));
        jobFailureNotificationService.notifyFailure(toReport(jobExecution));
    }

    private JobFailureReport toReport(JobExecution jobExecution) {
        List<String> failedSteps = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStatus() != BatchStatus.COMPLETED)
                .map(StepExecution::getStepName)
                .toList();

        long skipped = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getSkipCount)
                .sum();
        long written = jobExecution.getStepExecutions().stream()
                .mapToLong(StepExecution::getWriteCount)
                .sum();

        return new JobFailureReport(
                jobExecution.getJobInstance().getJobName(),
                jobExecution.getId(),
                jobExecution.getJobParameters().getString(JobParameterNames.TARGET_DATE),
                jobExecution.getJobParameters().getString(JobParameterNames.INPUT_FILE),
                jobExecution.getStatus().toString(),
                failedSteps,
                skipped,
                written,
                failureMessages(jobExecution));
    }

    private List<String> failureMessages(JobExecution jobExecution) {
        List<String> messages = new ArrayList<>();
        jobExecution.getAllFailureExceptions().stream()
                .map(JobFailureNotificationListener::describe)
                .forEach(messages::add);

        if (messages.isEmpty() && !jobExecution.getExitStatus().getExitDescription().isBlank()) {
            messages.add(jobExecution.getExitStatus().getExitDescription());
        }
        return messages;
    }

    /**
     * 例外を原因までたどって 1 行に連ねる。
     *
     * <p>一番外側の例外はたいてい役に立たない（ヘッダ不一致も「Failed to initialize
     * the reader」になってしまう）。運用チームがログを開かずに原因を掴めるよう、
     * 原因の連鎖も添える。
     */
    private static String describe(Throwable throwable) {
        StringBuilder description = new StringBuilder();
        Set<Throwable> visited = Collections.newSetFromMap(new IdentityHashMap<>());

        Throwable current = throwable;
        for (int depth = 0; current != null && depth < MAX_CAUSE_DEPTH; depth++) {
            if (!visited.add(current)) {
                // 循環した原因チェーンで無限に回らないようにする。
                break;
            }
            if (depth > 0) {
                description.append("\n      原因: ");
            }
            description.append(current.getClass().getSimpleName())
                    .append(": ")
                    .append(current.getMessage());
            current = current.getCause();
        }
        return description.toString();
    }

    private String summarize(JobExecution jobExecution) {
        return jobExecution.getStepExecutions().stream()
                .map(StepExecution::getSummary)
                .reduce((left, right) -> left + " | " + right)
                .orElse("(実行されたステップなし)");
    }
}
