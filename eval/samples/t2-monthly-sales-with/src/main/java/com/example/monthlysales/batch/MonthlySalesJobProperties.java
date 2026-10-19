package com.example.monthlysales.batch;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 月次売上集計ジョブの設定値。
 *
 * @param chunkSize 一度にまとめて処理・コミットする取引の件数
 * @param skipLimit 飛ばしてよい不正データの上限件数。超えるとジョブを失敗させる
 * @param deadlockRetryLimit デッドロック時の 1 まとまりあたりの試行回数の上限（初回を含む）
 */
@ConfigurationProperties(prefix = "batch.monthly-sales")
public record MonthlySalesJobProperties(
        @DefaultValue("1000") int chunkSize,
        @DefaultValue("1000") int skipLimit,
        @DefaultValue("3") int deadlockRetryLimit) {

    public MonthlySalesJobProperties {
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("chunk-size は 1 以上である必要がある: " + chunkSize);
        }
        if (skipLimit < 0) {
            throw new IllegalArgumentException("skip-limit は 0 以上である必要がある: " + skipLimit);
        }
        if (deadlockRetryLimit <= 0) {
            throw new IllegalArgumentException(
                    "deadlock-retry-limit は 1 以上である必要がある: " + deadlockRetryLimit);
        }
    }
}
