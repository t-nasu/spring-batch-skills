package com.example.orderimport.batch;

import com.example.orderimport.application.AggregateDailySalesUseCase;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.scope.context.ChunkContext;
import org.springframework.batch.core.step.StepContribution;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 日次売上の集計をユースケースに任せるだけの入れ物。
 */
@Component
@StepScope
public class AggregateDailySalesTasklet implements Tasklet {

    private final AggregateDailySalesUseCase aggregateDailySalesUseCase;
    private final String targetDate;

    public AggregateDailySalesTasklet(
            AggregateDailySalesUseCase aggregateDailySalesUseCase,
            @Value("#{jobParameters['targetDate']}") String targetDate) {
        this.aggregateDailySalesUseCase = aggregateDailySalesUseCase;
        this.targetDate = targetDate;
    }

    @Override
    public RepeatStatus execute(StepContribution contribution, ChunkContext chunkContext) {
        aggregateDailySalesUseCase.execute(JobArguments.toBusinessDate(targetDate));
        return RepeatStatus.FINISHED;
    }
}
