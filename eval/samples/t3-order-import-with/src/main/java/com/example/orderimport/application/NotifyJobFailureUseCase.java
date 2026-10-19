package com.example.orderimport.application;

import com.example.orderimport.domain.notification.JobFailureNotice;
import com.example.orderimport.domain.notification.NotificationGateway;
import org.springframework.stereotype.Component;

/**
 * ジョブの失敗を運用チームへ知らせる（仕様 4-4）。
 */
@Component
public class NotifyJobFailureUseCase {

    private final NotificationGateway notificationGateway;

    public NotifyJobFailureUseCase(NotificationGateway notificationGateway) {
        this.notificationGateway = notificationGateway;
    }

    public void execute(JobFailureNotice notice) {
        notificationGateway.notifyFailure(notice);
    }
}
