package com.example.orderimport.support;

import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.step.StepExecution;

/**
 * ジョブ実行から件数を取り出す小道具。
 */
public final class JobCounters {

    private JobCounters() {
    }

    public static long skipCountOf(JobExecution execution) {
        return execution.getStepExecutions().stream()
                .mapToLong(StepExecution::getSkipCount)
                .sum();
    }
}
