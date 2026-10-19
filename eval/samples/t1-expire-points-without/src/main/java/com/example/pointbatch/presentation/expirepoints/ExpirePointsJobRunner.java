package com.example.pointbatch.presentation.expirepoints;

import com.example.pointbatch.application.expirepoints.ExpirePointsResult;
import com.example.pointbatch.application.expirepoints.ExpirePointsUseCase;
import com.example.pointbatch.presentation.ExitCode;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * expirePointsJob の起動口。
 *
 * <p>例外は全てここで捕捉し、終了コードに変換してスケジューラに返す。
 * 仕様どおり再試行はせず、異常時はジョブを止めてスケジューラの監視に委ねる。
 */
@Component
@Profile("!test")
public class ExpirePointsJobRunner implements CommandLineRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(ExpirePointsJobRunner.class);
    private static final String JOB_NAME = "expirePointsJob";

    private final ExpirePointsUseCase expirePointsUseCase;
    private ExitCode exitCode = ExitCode.SUCCESS;

    public ExpirePointsJobRunner(ExpirePointsUseCase expirePointsUseCase) {
        this.expirePointsUseCase = expirePointsUseCase;
    }

    @Override
    public void run(String... args) {
        this.exitCode = ExitCode.SUCCESS;

        ExpirePointsJobArguments arguments;
        try {
            arguments = ExpirePointsJobArguments.parse(args);
        } catch (JobArgumentException e) {
            log.error("[{}] 起動引数が不正です: {}", JOB_NAME, e.getMessage());
            this.exitCode = ExitCode.INVALID_ARGUMENT;
            return;
        }

        Instant startedAt = Instant.now();
        log.info("[{}] 開始しました: targetDate={}", JOB_NAME, arguments.targetDate());
        try {
            ExpirePointsResult result = expirePointsUseCase.execute(arguments.targetDate());
            log.info(
                    "[{}] 正常終了しました: targetDate={}, 失効件数={}, 失効ポイント合計={}, コミット回数={}, 所要時間={}ms",
                    JOB_NAME,
                    arguments.targetDate(),
                    result.expiredPointCount(),
                    result.expiredAmountTotal(),
                    result.chunkCount(),
                    elapsedMillis(startedAt));
        } catch (Exception e) {
            // 仕様: 一時的なエラーも再試行しない。途中までの失効はコミット済みだが、
            //       再実行時に未処理分だけが抽出されるため最初からやり直してよい。
            log.error(
                    "[{}] 異常終了しました: targetDate={}, 所要時間={}ms",
                    JOB_NAME,
                    arguments.targetDate(),
                    elapsedMillis(startedAt),
                    e);
            this.exitCode = ExitCode.FAILED;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode.value();
    }

    private static long elapsedMillis(Instant startedAt) {
        return Duration.between(startedAt, Instant.now()).toMillis();
    }
}
