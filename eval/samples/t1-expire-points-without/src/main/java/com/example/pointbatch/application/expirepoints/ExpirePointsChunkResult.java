package com.example.pointbatch.application.expirepoints;

import com.example.pointbatch.domain.model.point.MemberPointId;
import java.util.Optional;

/**
 * 1 チャンク分の処理結果。
 *
 * @param processedCount    このチャンクで失効させた件数
 * @param amountTotal       このチャンクで失効させたポイント数の合計
 * @param lastProcessedId   このチャンクで処理した最大の会員ポイント ID。0 件の場合は空。
 */
public record ExpirePointsChunkResult(
        int processedCount, long amountTotal, Optional<MemberPointId> lastProcessedId) {

    public static ExpirePointsChunkResult empty() {
        return new ExpirePointsChunkResult(0, 0L, Optional.empty());
    }

    public boolean isEmpty() {
        return processedCount == 0;
    }
}
