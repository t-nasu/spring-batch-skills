package com.example.orderimport.batch;

import com.example.orderimport.domain.shared.BusinessDate;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersValidator;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * 起動引数の解釈（仕様 2）。
 *
 * <pre>
 * java -jar order-import-batch.jar targetDate=20261018 inputFile=/data/in/orders_20261018.csv
 * </pre>
 */
public final class JobArguments {

    public static final String TARGET_DATE = "targetDate";
    public static final String INPUT_FILE = "inputFile";

    private static final DateTimeFormatter TARGET_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private JobArguments() {
    }

    /**
     * {@code targetDate=yyyyMMdd} を業務日付にする。
     */
    public static BusinessDate toBusinessDate(String rawTargetDate) {
        try {
            return BusinessDate.of(LocalDate.parse(rawTargetDate, TARGET_DATE_FORMAT));
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException(TARGET_DATE + " は yyyyMMdd 形式である必要がある: " + rawTargetDate, e);
        }
    }

    /**
     * 起動引数が揃っているかを、ジョブを動かす前に確かめる。
     */
    public static JobParametersValidator validator() {
        return parameters -> {
            if (parameters == null) {
                throw new InvalidJobParametersException("起動引数が指定されていない");
            }
            requireText(parameters, TARGET_DATE);
            requireText(parameters, INPUT_FILE);
            try {
                toBusinessDate(parameters.getString(TARGET_DATE));
            } catch (IllegalArgumentException e) {
                throw new InvalidJobParametersException(e.getMessage());
            }
        };
    }

    private static void requireText(JobParameters parameters, String key) throws InvalidJobParametersException {
        String value = parameters.getString(key);
        if (value == null || value.isBlank()) {
            throw new InvalidJobParametersException(key + " は必須の起動引数である");
        }
    }
}
