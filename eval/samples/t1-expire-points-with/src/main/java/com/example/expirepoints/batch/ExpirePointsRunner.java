package com.example.expirepoints.batch;

import com.example.expirepoints.application.ExpirePointsResult;
import com.example.expirepoints.application.ExpirePointsUseCase;
import com.example.expirepoints.domain.BusinessDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.ExitCodeGenerator;
import org.springframework.stereotype.Component;

/**
 * expirePointsJob の起動口。
 *
 * <p>起動引数を解釈してユースケースを一度だけ呼び、成否を終了コードで返す。業務判断はここに
 * 書かない。
 */
@Component
public class ExpirePointsRunner implements ApplicationRunner, ExitCodeGenerator {

    private static final Logger log = LoggerFactory.getLogger(ExpirePointsRunner.class);

    private static final int EXIT_SUCCESS = 0;
    private static final int EXIT_FAILURE = 1;

    private final ExpirePointsUseCase expirePointsUseCase;

    private int exitCode = EXIT_SUCCESS;

    public ExpirePointsRunner(ExpirePointsUseCase expirePointsUseCase) {
        this.expirePointsUseCase = expirePointsUseCase;
    }

    @Override
    public void run(ApplicationArguments args) {
        BusinessDate businessDate;
        try {
            businessDate = TargetDateArgument.from(args);
        } catch (IllegalArgumentException e) {
            log.error("expirePointsJob の起動引数が不正である", e);
            exitCode = EXIT_FAILURE;
            return;
        }

        log.info("expirePointsJob を開始する: targetDate={}", businessDate);
        try {
            ExpirePointsResult result = expirePointsUseCase.execute(businessDate);
            log.info(
                    "expirePointsJob が正常終了した: targetDate={}, expiredCount={}, totalExpiredPoints={}",
                    businessDate,
                    result.expiredCount(),
                    result.totalExpiredPoints());
        } catch (RuntimeException e) {
            // 仕様どおり再試行はしない。処理は巻き戻っているので、原因を直して最初からやり直す。
            log.error("expirePointsJob が異常終了した: targetDate={}", businessDate, e);
            exitCode = EXIT_FAILURE;
        }
    }

    @Override
    public int getExitCode() {
        return exitCode;
    }
}
