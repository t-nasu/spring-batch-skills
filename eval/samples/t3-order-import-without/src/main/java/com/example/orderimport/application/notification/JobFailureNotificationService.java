package com.example.orderimport.application.notification;

import com.example.orderimport.application.port.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * ジョブが異常終了したことを運用チームへ通知するユースケース。
 *
 * <p>文面の組み立てをここに集約し、呼び出し側（ジョブのリスナー）は状況の収集だけを
 * 行う。通知手段そのものは {@link NotificationGateway} の実装に委ねる。
 */
@Service
public class JobFailureNotificationService {

    private static final Logger log = LoggerFactory.getLogger(JobFailureNotificationService.class);

    private final NotificationGateway notificationGateway;

    public JobFailureNotificationService(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    public void notifyFailure(JobFailureReport report) {
        NotificationGateway.Notification notification = new NotificationGateway.Notification(
                NotificationGateway.Severity.ERROR,
                buildSubject(report),
                buildBody(report));

        // 通知の失敗でジョブの結末を上書きしないよう、ここで握り潰して記録に留める。
        try {
            notificationGateway.send(notification);
        } catch (RuntimeException e) {
            log.error("運用チームへの通知に失敗しました。件名: {}", notification.subject(), e);
        }
    }

    private String buildSubject(JobFailureReport report) {
        return "[異常終了] %s (業務日付=%s)".formatted(report.jobName(), report.businessDate());
    }

    private String buildBody(JobFailureReport report) {
        StringBuilder body = new StringBuilder();
        body.append("バッチが異常終了しました。\n\n");
        body.append("ジョブ名        : ").append(report.jobName()).append('\n');
        body.append("ジョブ実行 ID   : ").append(report.jobExecutionId()).append('\n');
        body.append("業務日付        : ").append(report.businessDate()).append('\n');
        body.append("入力ファイル    : ").append(report.inputFile()).append('\n');
        body.append("終了ステータス  : ").append(report.status()).append('\n');
        body.append("異常終了ステップ: ").append(String.join(", ", report.failedSteps())).append('\n');
        body.append("登録済み注文件数: ").append(report.writtenCount()).append('\n');
        body.append("読み飛ばし件数  : ").append(report.skippedCount()).append('\n');
        body.append('\n');
        body.append("原因:\n");
        if (report.failureMessages().isEmpty()) {
            body.append("  （例外情報なし。ログを確認してください）\n");
        } else {
            report.failureMessages().forEach(message -> body.append("  - ").append(message).append('\n'));
        }
        body.append('\n');
        body.append("対処:\n");
        body.append("  原因を解消したうえで、同じ引数のまま再実行してください。\n");
        body.append("  失敗したステップの、最後にコミットされた地点から再開します。\n");
        return body.toString();
    }
}
