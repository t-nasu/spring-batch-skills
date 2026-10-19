package com.example.orderimport.domain.notification;

/**
 * 運用チームへの通知の送り口（仕様 6）。
 * 実装は infrastructure に置く。
 */
public interface NotificationGateway {

    void notifyFailure(JobFailureNotice notice);
}
