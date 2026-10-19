package com.example.orderimport.infrastructure.notification;

import com.example.orderimport.domain.notification.JobFailureNotice;
import com.example.orderimport.domain.notification.NotificationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * 運用チームへの通知の仮実装（仕様 6「実装は仮でよい」）。
 *
 * <p>いまはログに出すだけである。メール送信に差し替えるときは、この実装を置き換えればよく、
 * application 層・domain 層には手を入れない。
 */
@Component
public class LoggingNotificationGateway implements NotificationGateway {

    private static final Logger log = LoggerFactory.getLogger(LoggingNotificationGateway.class);

    @Override
    public void notifyFailure(JobFailureNotice notice) {
        log.error("運用チームへの通知（仮実装のためメールは送っていない）: job={}, targetDate={}, failedStep={}, reason={}",
                notice.jobName(), notice.targetDate(), notice.failedStepName(), notice.reason());
    }
}
