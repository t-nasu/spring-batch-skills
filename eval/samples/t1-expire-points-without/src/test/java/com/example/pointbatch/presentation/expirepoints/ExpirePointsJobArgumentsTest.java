package com.example.pointbatch.presentation.expirepoints;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ExpirePointsJobArgumentsTest {

    @Test
    @DisplayName("targetDate=yyyyMMdd を解釈する")
    void parsesPlainForm() {
        ExpirePointsJobArguments arguments = ExpirePointsJobArguments.parse("targetDate=20260401");

        assertThat(arguments.targetDate()).isEqualTo(BusinessDate.of(LocalDate.of(2026, 4, 1)));
    }

    @Test
    @DisplayName("--targetDate=yyyyMMdd 形式も受け付ける")
    void parsesOptionForm() {
        ExpirePointsJobArguments arguments = ExpirePointsJobArguments.parse("--targetDate=20260401");

        assertThat(arguments.targetDate()).isEqualTo(BusinessDate.of(LocalDate.of(2026, 4, 1)));
    }

    @Test
    @DisplayName("関係ない引数は無視する")
    void ignoresOtherArguments() {
        ExpirePointsJobArguments arguments =
                ExpirePointsJobArguments.parse("--spring.profiles.active=prod", "targetDate=20260401");

        assertThat(arguments.targetDate()).isEqualTo(BusinessDate.of(LocalDate.of(2026, 4, 1)));
    }

    @Test
    @DisplayName("targetDate が無い場合はエラー")
    void rejectsMissingArgument() {
        assertThatThrownBy(ExpirePointsJobArguments::parse)
                .isInstanceOf(JobArgumentException.class)
                .hasMessageContaining("指定されていません");
    }

    @Test
    @DisplayName("targetDate が複数指定された場合はエラー")
    void rejectsDuplicatedArgument() {
        assertThatThrownBy(() -> ExpirePointsJobArguments.parse("targetDate=20260401", "targetDate=20260402"))
                .isInstanceOf(JobArgumentException.class)
                .hasMessageContaining("複数指定");
    }

    @Test
    @DisplayName("targetDate の形式が不正な場合はエラー")
    void rejectsMalformedArgument() {
        assertThatThrownBy(() -> ExpirePointsJobArguments.parse("targetDate=2026-04-01"))
                .isInstanceOf(JobArgumentException.class)
                .hasMessageContaining("yyyyMMdd");
    }
}
