package com.example.expirepoints.application;

import com.example.expirepoints.domain.BusinessDate;
import com.example.expirepoints.domain.MemberPoint;
import com.example.expirepoints.domain.MemberPointRepository;
import com.example.expirepoints.domain.PointExpirationHistory;
import com.example.expirepoints.domain.PointExpirationHistoryRepository;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 有効期限を過ぎた会員ポイントを失効させ、失効履歴を残す。
 *
 * <p>全件を 1 トランザクションで処理する。最大想定件数は 20,000 件であり、1 トランザクションに
 * 収まる。途中で失敗したときは全件巻き戻るため、最初からやり直せる。
 */
@Component
public class ExpirePointsUseCase {

    private static final Logger log = LoggerFactory.getLogger(ExpirePointsUseCase.class);

    private final MemberPointRepository memberPointRepository;
    private final PointExpirationHistoryRepository pointExpirationHistoryRepository;

    public ExpirePointsUseCase(
            MemberPointRepository memberPointRepository,
            PointExpirationHistoryRepository pointExpirationHistoryRepository) {
        this.memberPointRepository = memberPointRepository;
        this.pointExpirationHistoryRepository = pointExpirationHistoryRepository;
    }

    @Transactional
    public ExpirePointsResult execute(BusinessDate businessDate) {
        List<MemberPoint> targets = memberPointRepository.findExpirableAsOf(businessDate);
        log.info("失効対象を抽出した: businessDate={}, count={}", businessDate, targets.size());

        if (targets.isEmpty()) {
            return ExpirePointsResult.none();
        }

        List<MemberPoint> expiredPoints = new ArrayList<>(targets.size());
        List<PointExpirationHistory> histories = new ArrayList<>(targets.size());
        long totalPoints = 0L;
        for (MemberPoint target : targets) {
            MemberPoint expired = target.expire(businessDate);
            expiredPoints.add(expired);
            histories.add(PointExpirationHistory.of(expired, businessDate));
            totalPoints += expired.amount().value();
        }

        int updated = memberPointRepository.saveExpired(expiredPoints);
        if (updated != expiredPoints.size()) {
            throw new IllegalStateException(
                    "失効の更新件数が対象件数と一致しない: expected=%d, actual=%d"
                            .formatted(expiredPoints.size(), updated));
        }

        int inserted = pointExpirationHistoryRepository.addAll(histories);
        if (inserted != histories.size()) {
            throw new IllegalStateException(
                    "失効履歴の登録件数が対象件数と一致しない: expected=%d, actual=%d"
                            .formatted(histories.size(), inserted));
        }

        log.info("失効させた: count={}, totalPoints={}", updated, totalPoints);
        return new ExpirePointsResult(updated, totalPoints);
    }
}
