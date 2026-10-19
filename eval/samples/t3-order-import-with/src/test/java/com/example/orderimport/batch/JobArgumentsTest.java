package com.example.orderimport.batch;

import com.example.orderimport.domain.shared.BusinessDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JobArgumentsTest {

    @Test
    @DisplayName("業務日付を yyyyMMdd として読む")
    void readsTargetDateAsYyyyMMdd() {
        assertThat(JobArguments.toBusinessDate("20261018"))
                .isEqualTo(BusinessDate.of(LocalDate.of(2026, 10, 18)));
    }

    @Test
    @DisplayName("業務日付の形式が違えば弾く")
    void rejectsMalformedTargetDate() {
        assertThatThrownBy(() -> JobArguments.toBusinessDate("2026-10-18"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("yyyyMMdd");
    }

    @Test
    @DisplayName("起動引数が揃っていれば通る")
    void acceptsCompleteArguments() {
        assertThatCode(() -> JobArguments.validator().validate(parameters("20261018", "/data/in/orders.csv")))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("入力ファイルの指定がなければ弾く")
    void rejectsMissingInputFile() {
        assertThatThrownBy(() -> JobArguments.validator().validate(parameters("20261018", null)))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining(JobArguments.INPUT_FILE);
    }

    @Test
    @DisplayName("業務日付の指定がなければ弾く")
    void rejectsMissingTargetDate() {
        assertThatThrownBy(() -> JobArguments.validator().validate(parameters(null, "/data/in/orders.csv")))
                .isInstanceOf(InvalidJobParametersException.class)
                .hasMessageContaining(JobArguments.TARGET_DATE);
    }

    @Test
    @DisplayName("業務日付の形式が違う起動引数は弾く")
    void rejectsMalformedTargetDateArgument() {
        assertThatThrownBy(() -> JobArguments.validator().validate(parameters("2026-10-18", "/data/in/orders.csv")))
                .isInstanceOf(InvalidJobParametersException.class);
    }

    private static JobParameters parameters(String targetDate, String inputFile) {
        JobParametersBuilder builder = new JobParametersBuilder();
        if (targetDate != null) {
            builder.addString(JobArguments.TARGET_DATE, targetDate);
        }
        if (inputFile != null) {
            builder.addString(JobArguments.INPUT_FILE, inputFile);
        }
        return builder.toJobParameters();
    }
}
