package com.example.salesbatch.presentation.batch.tasklet;

import com.example.salesbatch.application.service.MonthlySalesSummaryRegistrationService;
import com.example.salesbatch.domain.model.TargetMonth;

import java.util.Objects;

import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;

/**
 * 対象年月の既存の集計結果を削除する前処理。
 *
 * <p>作り直しのための削除なので、ジョブ実行（ジョブインスタンス）につき 1 度だけ実行されればよい。
 * このステップは成功すると完了済みとして記録され、再開時には実行されない
 * （{@code allowStartIfComplete} は既定の false のまま）。
 * そのため、再開しても登録済みの集計結果が消えることはない。</p>
 */
public class PurgeMonthlySalesSummaryTasklet implements Tasklet {

    private final MonthlySalesSummaryRegistrationService registrationService;
    private final TargetMonth targetMonth;

    public PurgeMonthlySalesSummaryTasklet(
            MonthlySalesSummaryRegistrationService registrationService, TargetMonth targetMonth) {
        this.registrationService = Objects.requireNonNull(registrationService, "registrationService は必須です");
        this.targetMonth = Objects.requireNonNull(targetMonth, "targetMonth は必須です");
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        int deleted = registrationService.purge(targetMonth);
        contribution.incrementWriteCount(deleted);
        return RepeatStatus.FINISHED;
    }
}
