package com.example.salesbatch.application.port;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

/**
 * 運用チームへ通知する異常終了の内容。
 *
 * @param jobName        ジョブ名
 * @param targetMonth    対象年月（{@code yyyyMM}）
 * @param jobExecutionId ジョブ実行 ID（再開時に指定する）
 * @param occurredAt     検知時刻
 * @param failureMessages 失敗の内容（例外のメッセージ）
 */
public record BatchFailureReport(
        String jobName,
        String targetMonth,
        Long jobExecutionId,
        LocalDateTime occurredAt,
        List<String> failureMessages) {

    public BatchFailureReport {
        Objects.requireNonNull(jobName, "jobName は必須です");
        Objects.requireNonNull(occurredAt, "occurredAt は必須です");
        failureMessages = List.copyOf(Objects.requireNonNullElse(failureMessages, List.<String>of()));
    }
}
