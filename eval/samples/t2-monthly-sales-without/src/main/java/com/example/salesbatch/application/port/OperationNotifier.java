package com.example.salesbatch.application.port;

/**
 * 運用チームへの通知ポート。通知手段（メール / チャット / 監視基盤）はインフラ層の実装で差し替える。
 */
public interface OperationNotifier {

    /** ジョブが異常終了したことを通知する。 */
    void notifyFailure(BatchFailureReport report);
}
