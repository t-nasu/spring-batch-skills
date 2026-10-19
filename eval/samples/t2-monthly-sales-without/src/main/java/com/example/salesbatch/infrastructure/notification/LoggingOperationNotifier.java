package com.example.salesbatch.infrastructure.notification;

import com.example.salesbatch.application.port.BatchFailureReport;
import com.example.salesbatch.application.port.OperationNotifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 運用チームへの通知をログ出力で行う実装。
 *
 * <p>{@code OPS-ALERT} を含む ERROR ログを監視基盤（ログ監視・アラート）に拾わせて通知する前提。
 * メールやチャットに直接送る場合は {@link OperationNotifier} の別実装に差し替える。</p>
 */
@Component
public class LoggingOperationNotifier implements OperationNotifier {

    private static final Logger log = LoggerFactory.getLogger(LoggingOperationNotifier.class);

    @Override
    public void notifyFailure(BatchFailureReport report) {
        log.error("""
                [OPS-ALERT] バッチが異常終了しました
                  ジョブ名       : {}
                  対象年月       : {}
                  ジョブ実行 ID  : {}
                  検知時刻       : {}
                  失敗の内容     : {}
                  再開方法       : 同じ起動引数 (targetMonth={}) で再実行すると、中断した箇所から再開します""",
                report.jobName(),
                report.targetMonth(),
                report.jobExecutionId(),
                report.occurredAt(),
                report.failureMessages(),
                report.targetMonth());
    }
}
