package com.example.orderimport.batch;

import com.example.orderimport.application.NotifyJobFailureUseCase;
import com.example.orderimport.domain.notification.JobFailureNotice;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

/**
 * 失敗したステップを調べ、知らせの内容を組み立ててユースケースに渡す。
 */
@Component
@StepScope
public class NotifyFailureTasklet implements Tasklet {

    private static final String UNKNOWN = "(不明)";

    private final NotifyJobFailureUseCase notifyJobFailureUseCase;
    private final String targetDate;

    public NotifyFailureTasklet(
            NotifyJobFailureUseCase notifyJobFailureUseCase,
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        this.notifyJobFailureUseCase = notifyJobFailureUseCase;
        this.targetDate = targetDate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        StepExecution thisStep = chunkContext.getStepContext().getStepExecution();
        JobExecution jobExecution = thisStep.getJobExecution();

        StepExecution failed = jobExecution.getStepExecutions().stream()
                .filter(step -> ExitStatus.FAILED.getExitCode().equals(step.getExitStatus().getExitCode()))
                .findFirst()
                .orElse(null);

        notifyJobFailureUseCase.execute(new JobFailureNotice(
                chunkContext.getStepContext().getJobName(),
                JobArguments.toBusinessDate(targetDate),
                failed == null ? UNKNOWN : failed.getStepName(),
                failed == null ? UNKNOWN : describe(failed)));

        return RepeatStatus.FINISHED;
    }

    private static String describe(StepExecution failed) {
        String causes = failed.getFailureExceptions().stream()
                .map(e -> e.getClass().getSimpleName() + ": " + e.getMessage())
                .collect(Collectors.joining(" / "));
        return "読み込み %d 件, 書き込み %d 件, 読み飛ばし %d 件. 原因: %s".formatted(
                failed.getReadCount(),
                failed.getWriteCount(),
                failed.getSkipCount(),
                causes.isEmpty() ? UNKNOWN : causes);
    }
}
