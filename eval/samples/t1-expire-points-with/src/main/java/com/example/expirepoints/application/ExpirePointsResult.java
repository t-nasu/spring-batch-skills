package com.example.expirepoints.application;

/**
 * 失効処理の結果。
 *
 * @param expiredCount 失効させた会員ポイントの件数
 * @param totalExpiredPoints 失効させたポイント数の合計
 */
public record ExpirePointsResult(int expiredCount, long totalExpiredPoints) {

    public static ExpirePointsResult none() {
        return new ExpirePointsResult(0, 0L);
    }
}
