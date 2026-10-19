package com.example.monthlysales.batch;

import com.example.monthlysales.domain.sales.InvalidSalesTransactionException;
import com.example.monthlysales.domain.sales.SalesTransaction;
import com.example.monthlysales.domain.summary.MonthlySalesSummary;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

/** 飛ばした取引の ID と理由をログに残す。 */
public class SkippedSalesTransactionLogger
        implements SkipListener<SalesTransaction, MonthlySalesSummary> {

    private static final Logger log = LoggerFactory.getLogger(SkippedSalesTransactionLogger.class);

    /** DB から読んだ値がドメインの決まりを満たさなかった場合。 */
    @Override
    public void onSkipInRead(Throwable cause) {
        logSkipped(cause);
    }

    /** 対象年月の外の取引など、ユースケースで不正と判断した場合。 */
    @Override
    public void onSkipInProcess(SalesTransaction item, Throwable cause) {
        logSkipped(cause);
    }

    @Override
    public void onSkipInWrite(MonthlySalesSummary item, Throwable cause) {
        log.warn("月次売上集計の書き込みを飛ばした: {}", item.key(), cause);
    }

    private void logSkipped(Throwable cause) {
        if (cause instanceof InvalidSalesTransactionException invalid) {
            log.warn("不正な取引を飛ばした: 取引 ID={}, 理由={}",
                    invalid.transactionId(), invalid.reason());
        } else {
            log.warn("取引を飛ばした", cause);
        }
    }
}
