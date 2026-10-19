package com.example.monthlysales.batch;

import com.example.monthlysales.domain.shared.TargetMonth;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;

/** 起動引数 {@code targetMonth=yyyyMM} を検査する。 */
public class TargetMonthJobParametersValidator implements JobParametersValidator {

    /** 対象年月を渡す起動引数の名前。識別用パラメータなので、同じ値での二重起動は防がれる。 */
    public static final String TARGET_MONTH = "targetMonth";

    @Override
    public void validate(JobParameters parameters) throws InvalidJobParametersException {
        String value = parameters == null ? null : parameters.getString(TARGET_MONTH);
        if (value == null || value.isBlank()) {
            throw new InvalidJobParametersException("起動引数 " + TARGET_MONTH + "=yyyyMM を指定する");
        }
        try {
            TargetMonth.parse(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidJobParametersException(e.getMessage());
        }
    }
}
