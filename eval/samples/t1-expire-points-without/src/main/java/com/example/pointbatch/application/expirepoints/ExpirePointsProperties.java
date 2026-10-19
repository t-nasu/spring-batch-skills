package com.example.pointbatch.application.expirepoints;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * ポイント失効ジョブの設定。
 *
 * @param chunkSize 1 トランザクションで処理する件数
 */
@ConfigurationProperties(prefix = "batch.expire-points")
public record ExpirePointsProperties(Integer chunkSize) {

    private static final int DEFAULT_CHUNK_SIZE = 1000;

    public ExpirePointsProperties {
        if (chunkSize == null) {
            chunkSize = DEFAULT_CHUNK_SIZE;
        }
        if (chunkSize <= 0) {
            throw new IllegalArgumentException("batch.expire-points.chunk-size は 1 以上で指定してください: " + chunkSize);
        }
    }
}
