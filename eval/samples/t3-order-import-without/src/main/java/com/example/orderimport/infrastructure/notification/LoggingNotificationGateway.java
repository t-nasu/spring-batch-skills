package com.example.orderimport.infrastructure.notification;

import com.example.orderimport.application.port.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 通知内容をログに出力するだけの仮実装。
 *
 * <p>仕様上の通知手段はメールだが、送信基盤（SMTP の接続先、宛先アドレスの管理、
 * 再送の扱い）が未定のため本実装は後続課題としている。それまでの間は、通知される
 * はずだった内容が運用ログに必ず残るようにしておく。
 *
 * <p>メール実装に差し替えるときは、{@link NotificationGateway} を実装したクラスを
 * このクラスと入れ替えるだけでよい。アプリケーション層はポートにしか依存して
 * いないため、呼び出し側に変更は生じない。
 */
@Component
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    public LoggingNotificationGateway() {
        log.warn("通知は仮実装（ログ出力のみ）で動作します。運用チームへのメールは送信されません");
    }

    @Override
    public void send(Notification notification) {
        String rendered = """

                ---------------- 運用チーム宛通知（未送信・ログ出力のみ）----------------
                重要度: %s
                件名  : %s
                %s
                ----------------------------------------------------------------------"""
                .formatted(notification.severity(), notification.subject(), notification.body());

        switch (notification.severity()) {
            case ERROR -> log.error(rendered);
            case WARN -> log.warn(rendered);
        }
    }
}
