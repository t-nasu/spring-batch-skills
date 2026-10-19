package com.example.salesbatch.config;

import java.time.Duration;
import java.time.LocalTime;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 月次売上集計バッチの設定値（{@code batch.monthly-sales.*}）。
 *
 * @param chunkSize            1 トランザクションで登録する集計単位の件数
 * @param pageSize             取引明細を 1 回の SELECT で読み込む件数
 * @param skipLimit            不正データとして読み飛ばせる上限件数
 * @param deadlockRetryCount   デッドロック時の再試行回数（初回実行は含まない）
 * @param retryInitialInterval 再試行の初回待ち時間
 * @param retryMaxInterval     再試行の最大待ち時間
 * @param deadline             終了期限。超えた場合は WARN ログを出力する
 */
@ConfigurationProperties(prefix = "batch.monthly-sales")
public record MonthlySalesBatchProperties(
        @DefaultValue("500") int chunkSize,
        @DefaultValue("5000") int pageSize,
        @DefaultValue("1000") int skipLimit,
        @DefaultValue("3") int deadlockRetryCount,
        @DefaultValue("200ms") Duration retryInitialInterval,
        @DefaultValue("2s") Duration retryMaxInterval,
        @DefaultValue("06:00") LocalTime deadline) {

    public MonthlySalesBatchProperties {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("batch.monthly-sales.chunk-size は 1 以上です: " + chunkSize);
        }
        if (pageSize < 1) {
            throw new IllegalArgumentException("batch.monthly-sales.page-size は 1 以上です: " + pageSize);
        }
        if (skipLimit < 0) {
            throw new IllegalArgumentException("batch.monthly-sales.skip-limit は 0 以上です: " + skipLimit);
        }
        if (deadlockRetryCount < 0) {
            throw new IllegalArgumentException(
                    "batch.monthly-sales.deadlock-retry-count は 0 以上です: " + deadlockRetryCount);
        }
    }
}
