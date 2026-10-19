package com.example.monthlysales.domain.notification;

/** バッチが異常終了したことを運用チームへ伝える内容。 */
public record JobFailureNotice(String jobName, String targetMonth, String reason) {
}
