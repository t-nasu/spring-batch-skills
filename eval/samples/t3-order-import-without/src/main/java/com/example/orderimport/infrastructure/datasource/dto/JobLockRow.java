package com.example.orderimport.infrastructure.datasource.dto;

import java.time.LocalDateTime;

/**
 * {@code batch_job_lock} の 1 行。
 */
public record JobLockRow(
        String jobName,
        String ownerId,
        LocalDateTime lockedAt) {
}
