package com.example.monthlysales.application;

import com.example.monthlysales.domain.notification.JobFailureNotice;
import com.example.monthlysales.domain.notification.OperationTeamNotifier;
import org.springframework.stereotype.Component;

/** バッチの異常終了を運用チームへ知らせる。 */
@Component
public class NotifyJobFailureUseCase {

    private final OperationTeamNotifier notifier;

    public NotifyJobFailureUseCase(OperationTeamNotifier notifier) {
        this.notifier = notifier;
    }

    public void notifyFailure(String jobName, String targetMonth, String reason) {
        notifier.notifyFailure(new JobFailureNotice(jobName, targetMonth, reason));
    }
}
