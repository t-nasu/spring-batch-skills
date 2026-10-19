package com.example.monthlysales.domain.notification;

/** 運用チームへの通知先。 */
public interface OperationTeamNotifier {

    void notifyFailure(JobFailureNotice notice);
}
