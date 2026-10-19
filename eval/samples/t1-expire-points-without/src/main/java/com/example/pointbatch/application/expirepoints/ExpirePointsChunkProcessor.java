package com.example.pointbatch.application.expirepoints;

import com.example.pointbatch.domain.model.point.MemberPoint;
import com.example.pointbatch.domain.model.point.MemberPointId;
import com.example.pointbatch.domain.model.point.PointExpirationHistory;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import com.example.pointbatch.domain.repository.MemberPointRepository;
import com.example.pointbatch.domain.repository.PointExpirationHistoryRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * ポイント失効処理の 1 チャンク分を 1 トランザクションで実行する。
 *
 * <p>チャンクごとにコミットするため途中で異常終了しても一貫性は保たれる。
 * 失効済みのポイントは抽出条件 {@code status = 'ACTIVE'} から外れるので、
 * 仕様どおり「最初からやり直す」運用で二重失効は起こらない。
 */
@Component
public class ExpirePointsChunkProcessor {

    private final MemberPointRepository memberPointRepository;
    private final PointExpirationHistoryRepository pointExpirationHistoryRepository;

    public ExpirePointsChunkProcessor(
            MemberPointRepository memberPointRepository,
            PointExpirationHistoryRepository pointExpirationHistoryRepository) {
        this.memberPointRepository = memberPointRepository;
        this.pointExpirationHistoryRepository = pointExpirationHistoryRepository;
    }

    /**
     * 失効対象を 1 チャンク分取得して失効させる。
     *
     * @param businessDate     業務日付
     * @param exclusiveStartId この ID より大きいものを処理する。空の場合は先頭から。
     * @param chunkSize        1 チャンクの最大件数
     * @return チャンクの処理結果。対象が無い場合は {@link ExpirePointsChunkResult#empty()}。
     */
    @Transactional
    public ExpirePointsChunkResult process(
            BusinessDate businessDate, Optional<MemberPointId> exclusiveStartId, int chunkSize) {

        List<MemberPoint> targets =
                memberPointRepository.findExpirationTargets(businessDate, exclusiveStartId, chunkSize);
        if (targets.isEmpty()) {
            return ExpirePointsChunkResult.empty();
        }

        // 不正データ・不正状態があればここで例外となり、チャンクごとロールバックしてジョブを止める。
        List<PointExpirationHistory> histories = targets.stream().map(point -> point.expire(businessDate)).toList();

        memberPointRepository.saveExpired(targets);
        pointExpirationHistoryRepository.saveAll(histories);

        long amountTotal = histories.stream().mapToLong(history -> history.amount().value()).sum();
        MemberPointId lastProcessedId = targets.get(targets.size() - 1).id();
        return new ExpirePointsChunkResult(targets.size(), amountTotal, Optional.of(lastProcessedId));
    }
}
