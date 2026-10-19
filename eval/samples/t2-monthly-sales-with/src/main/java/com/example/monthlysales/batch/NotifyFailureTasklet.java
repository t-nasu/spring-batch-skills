package com.example.monthlysales.batch;

import com.example.monthlysales.application.NotifyJobFailureUseCase;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/** 失敗したステップの顛末をまとめ、運用チームへの通知を行う。 */
public class NotifyFailureTasklet implements Tasklet {

    private final NotifyJobFailureUseCase useCase;

    public NotifyFailureTasklet(NotifyJobFailureUseCase useCase) {
        this.useCase = useCase;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        JobExecution jobExecution =
                chunkContext.getStepContext().getStepExecution().getJobExecution();
        String targetMonth = jobExecution.getJobParameters()
                .getString(TargetMonthJobParametersValidator.TARGET_MONTH);
        useCase.notifyFailure(jobExecution.getJobInstance().getJobName(), targetMonth,
                reasonOf(jobExecution));
        return RepeatStatus.FINISHED;
    }

    /** 失敗したステップごとに「ステップ名: 原因」を並べた文字列を作る。 */
    private String reasonOf(JobExecution jobExecution) {
        String reason = jobExecution.getStepExecutions().stream()
                .filter(step -> step.getStatus().isUnsuccessful())
                .map(step -> step.getStepName() + ": " + causeOf(step.getFailureExceptions(),
                        step.getExitStatus().getExitDescription()))
                .collect(Collectors.joining(" / "));
        return reason.isEmpty() ? "原因を特定できなかった" : reason;
    }

    private String causeOf(List<Throwable> failures, String exitDescription) {
        if (!failures.isEmpty()) {
            return failures.getFirst().toString();
        }
        return exitDescription == null || exitDescription.isBlank() ? "(詳細なし)" : exitDescription;
    }
}
