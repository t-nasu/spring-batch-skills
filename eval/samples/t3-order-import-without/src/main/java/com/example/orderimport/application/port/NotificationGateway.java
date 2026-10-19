package com.example.orderimport.application.port;

/**
 * 運用チームへの通知を外部に送り出すためのポート。
 *
 * <p>仕様上の通知手段はメールだが、アプリケーション層はその事実に依存しない。
 * 実際の送信手段はインフラ層の実装が決める。
 */
public interface NotificationGateway {

    /**
     * 通知を送信する。
     *
     * <p>送信失敗でジョブの結果が覆らないよう、実装は例外を送出せず自身で記録する
     * ことが望ましい。
     */
    void send(Notification notification);

    /**
     * 通知内容。
     *
     * @param severity 重要度
     * @param subject  件名
     * @param body     本文
     */
    record Notification(Severity severity, String subject, String body) {
    }

    enum Severity {
        /** 運用チームの即時対応が必要。 */
        ERROR,
        /** 対応は必要だが緊急ではない。 */
        WARN
    }
}
