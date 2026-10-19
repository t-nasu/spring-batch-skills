package com.example.salesbatch.presentation.batch;

import com.example.salesbatch.domain.model.TargetMonth;

import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;

/**
 * 起動引数 {@code targetMonth=yyyyMM} の検証。
 */
public class TargetMonthJobParametersValidator implements JobParametersValidator {

    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        String targetMonth = parameters == null ? null : parameters.getString(MonthlySalesJobParameters.TARGET_MONTH);
        if (targetMonth == null) {
            throw new InvalidJobParametersException(
                    "起動引数 %s=yyyyMM は必須です".formatted(MonthlySalesJobParameters.TARGET_MONTH));
        }
        try {
            TargetMonth.of(targetMonth);
        } catch (IllegalArgumentException e) {
            throw new InvalidJobParametersException(e.getMessage());
        }
    }
}
