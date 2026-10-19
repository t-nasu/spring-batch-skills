package com.example.orderimport.application.notification;

import java.util.List;

/**
 * 異常終了したジョブの状況をまとめたもの。
 *
 * <p>Spring Batch の {@code JobExecution} をそのまま下位層へ渡さないための型。
 * 通知文面の組み立てに必要な情報だけを持つ。
 *
 * @param jobName         ジョブ名
 * @param jobExecutionId  ジョブ実行 ID。再開時に運用チームが参照する
 * @param businessDate    業務日付（{@code yyyyMMdd}）
 * @param inputFile       入力ファイルのパス
 * @param status          終了ステータス
 * @param failedSteps     異常終了したステップ名
 * @param skippedCount    読み飛ばした不正データの件数
 * @param writtenCount    登録できた注文の件数
 * @param failureMessages 失敗の原因となった例外のメッセージ
 */
public record JobFailureReport(
        String jobName,
        Long jobExecutionId,
        String businessDate,
        String inputFile,
        String status,
        List<String> failedSteps,
        long skippedCount,
        long writtenCount,
        List<String> failureMessages) {

    public JobFailureReport {
        failedSteps = List.copyOf(failedSteps);
        failureMessages = List.copyOf(failureMessages);
    }
}
