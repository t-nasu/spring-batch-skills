package com.example.orderimport.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * orderImportJob の調整値。
 *
 * @param chunkSize       1 トランザクションで登録する注文件数。再開時はこの単位まで
 *                        巻き戻る
 * @param skipLimit       読み飛ばせる不正データの上限件数。超えるとジョブは異常終了する
 * @param encoding        入力ファイルの文字コード
 * @param orderedAtFormat CSV の注文日時の書式
 * @param lockStaleAfter  この時間を超えた二重起動防止ロックは、異常終了の残骸と
 *                        みなして引き継ぐ
 */
@ConfigurationProperties(prefix = "batch.order-import")
public record OrderImportProperties(
        @DefaultValue("1000") int chunkSize,
        @DefaultValue("100") int skipLimit,
        @DefaultValue("UTF-8") String encoding,
        @DefaultValue("yyyy-MM-dd HH:mm:ss") String orderedAtFormat,
        @DefaultValue("PT6H") Duration lockStaleAfter) {

    public OrderImportProperties {
        if (chunkSize < 1) {
            throw new IllegalArgumentException("batch.order-import.chunk-size は 1 以上で指定してください: " + chunkSize);
        }
        if (skipLimit < 0) {
            throw new IllegalArgumentException("batch.order-import.skip-limit は 0 以上で指定してください: " + skipLimit);
        }
    }
}
