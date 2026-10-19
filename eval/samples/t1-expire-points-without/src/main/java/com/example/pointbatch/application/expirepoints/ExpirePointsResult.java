package com.example.pointbatch.application.expirepoints;

/**
 * ポイント失効ジョブの処理結果。
 *
 * @param expiredPointCount 失効させたポイントの件数
 * @param expiredAmountTotal 失効させたポイント数の合計
 * @param chunkCount         処理したチャンク(＝コミット)の数
 */
public record ExpirePointsResult(long expiredPointCount, long expiredAmountTotal, int chunkCount) {

    public static ExpirePointsResult empty() {
        return new ExpirePointsResult(0L, 0L, 0);
    }
}
