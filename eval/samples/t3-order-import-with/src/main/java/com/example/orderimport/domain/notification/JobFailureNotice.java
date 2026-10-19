package com.example.orderimport.domain.notification;

import com.example.orderimport.domain.shared.BusinessDate;

import java.util.Objects;

/**
 * ジョブが失敗したことの知らせ。
 */
public record JobFailureNotice(
        String jobName,
        BusinessDate targetDate,
        String failedStepName,
        String reason) {

    public JobFailureNotice {
        Objects.requireNonNull(jobName, "ジョブ名は必須である");
        Objects.requireNonNull(targetDate, "業務日付は必須である");
        Objects.requireNonNull(failedStepName, "失敗したステップ名は必須である");
        Objects.requireNonNull(reason, "失敗の理由は必須である");
    }
}
