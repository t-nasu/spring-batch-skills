package com.example.monthlysales.infrastructure.notification;

import com.example.monthlysales.domain.notification.JobFailureNotice;
import com.example.monthlysales.domain.notification.OperationTeamNotifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 運用チームへの通知をログで行う。
 *
 * <p>{@code OPS_ALERT} という名前のロガーへ ERROR で出す。実際の連絡手段（メール・チャットなど）は
 * このロガーの出力先として logback 側で設定し、アプリの作りは変えない。
 */
@Component
public class LogOperationTeamNotifier implements OperationTeamNotifier {

    private static final Logger OPS_ALERT = LoggerFactory.getLogger("OPS_ALERT");

    @Override
    public void notifyFailure(JobFailureNotice notice) {
        OPS_ALERT.error("バッチが異常終了した: ジョブ={}, 対象年月={}, 理由={}",
                notice.jobName(), notice.targetMonth(), notice.reason());
    }
}
