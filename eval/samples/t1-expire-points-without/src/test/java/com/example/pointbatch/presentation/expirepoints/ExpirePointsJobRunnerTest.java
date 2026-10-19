package com.example.pointbatch.presentation.expirepoints;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.pointbatch.application.expirepoints.ExpirePointsResult;
import com.example.pointbatch.application.expirepoints.ExpirePointsUseCase;
import com.example.pointbatch.domain.exception.InvalidPointDataException;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import com.example.pointbatch.presentation.ExitCode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * 起動口が例外を終了コードに正しく変換することを検証する。
 */
class ExpirePointsJobRunnerTest {

    @Test
    @DisplayName("正常終了した場合は終了コード 0 を返し、業務日付をユースケースに渡す")
    void returnsSuccess() {
        List<BusinessDate> received = new ArrayList<>();
        ExpirePointsUseCase useCase =
                businessDate -> {
                    received.add(businessDate);
                    return new ExpirePointsResult(10L, 1000L, 1);
                };
        ExpirePointsJobRunner runner = new ExpirePointsJobRunner(useCase);

        runner.run("targetDate=20260401");

        assertThat(runner.getExitCode()).isEqualTo(ExitCode.SUCCESS.value());
        assertThat(received).containsExactly(BusinessDate.of(LocalDate.of(2026, 4, 1)));
    }

    @Test
    @DisplayName("処理中に例外が発生した場合は再試行せず終了コード 1 を返す")
    void returnsFailed() {
        ExpirePointsUseCase useCase =
                businessDate -> {
                    throw new InvalidPointDataException("不正データ");
                };
        ExpirePointsJobRunner runner = new ExpirePointsJobRunner(useCase);

        runner.run("targetDate=20260401");

        assertThat(runner.getExitCode()).isEqualTo(ExitCode.FAILED.value());
    }

    @Test
    @DisplayName("起動引数が不正な場合はユースケースを呼ばず終了コード 2 を返す")
    void returnsInvalidArgument() {
        List<BusinessDate> received = new ArrayList<>();
        ExpirePointsUseCase useCase =
                businessDate -> {
                    received.add(businessDate);
                    return ExpirePointsResult.empty();
                };
        ExpirePointsJobRunner runner = new ExpirePointsJobRunner(useCase);

        runner.run();

        assertThat(runner.getExitCode()).isEqualTo(ExitCode.INVALID_ARGUMENT.value());
        assertThat(received).isEmpty();
    }

    @Test
    @DisplayName("終了コードは実行のたびに初期化される")
    void resetsExitCodePerRun() {
        ExpirePointsJobRunner runner =
                new ExpirePointsJobRunner(businessDate -> ExpirePointsResult.empty());

        runner.run();
        assertThat(runner.getExitCode()).isEqualTo(ExitCode.INVALID_ARGUMENT.value());

        runner.run("targetDate=20260401");
        assertThat(runner.getExitCode()).isEqualTo(ExitCode.SUCCESS.value());
    }
}
