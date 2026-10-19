package com.example.expirepoints.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.expirepoints.domain.BusinessDate;
import com.example.expirepoints.domain.MemberId;
import com.example.expirepoints.domain.MemberPoint;
import com.example.expirepoints.domain.MemberPointId;
import com.example.expirepoints.domain.MemberPointRepository;
import com.example.expirepoints.domain.PointAmount;
import com.example.expirepoints.domain.PointExpirationHistory;
import com.example.expirepoints.domain.PointExpirationHistoryRepository;
import com.example.expirepoints.domain.PointNotExpirableException;
import com.example.expirepoints.domain.PointStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ExpirePointsUseCaseTest {

    private static final BusinessDate TARGET_DATE = BusinessDate.parse("20261019");

    private final FakeMemberPointRepository memberPointRepository = new FakeMemberPointRepository();
    private final FakeHistoryRepository historyRepository = new FakeHistoryRepository();
    private final ExpirePointsUseCase useCase =
            new ExpirePointsUseCase(memberPointRepository, historyRepository);

    private static MemberPoint active(long id, long memberId, int points, LocalDate expiresOn) {
        return new MemberPoint(
                new MemberPointId(id),
                new MemberId(memberId),
                new PointAmount(points),
                expiresOn,
                PointStatus.ACTIVE);
    }

    @Test
    void 対象を失効させ_履歴を残す() {
        memberPointRepository.targets =
                List.of(
                        active(1L, 100L, 50, LocalDate.of(2026, 10, 18)),
                        active(2L, 100L, 30, LocalDate.of(2026, 1, 1)),
                        active(3L, 200L, 20, LocalDate.of(2026, 10, 18)));

        ExpirePointsResult result = useCase.execute(TARGET_DATE);

        assertThat(result.expiredCount()).isEqualTo(3);
        assertThat(result.totalExpiredPoints()).isEqualTo(100L);
        assertThat(memberPointRepository.saved)
                .hasSize(3)
                .allMatch(MemberPoint::isExpired);
        assertThat(historyRepository.added)
                .extracting(h -> h.memberPointId().value())
                .containsExactly(1L, 2L, 3L);
        assertThat(historyRepository.added)
                .allMatch(h -> h.expiredOn().equals(LocalDate.of(2026, 10, 19)));
    }

    @Test
    void 対象が無ければ何も書かない() {
        memberPointRepository.targets = List.of();

        ExpirePointsResult result = useCase.execute(TARGET_DATE);

        assertThat(result).isEqualTo(ExpirePointsResult.none());
        assertThat(memberPointRepository.saved).isEmpty();
        assertThat(historyRepository.added).isEmpty();
    }

    @Test
    void 失効対象でないものが混ざっていれば止まる() {
        // 抽出条件と実データが食い違った場合。ドメインが弾いてジョブを止める。
        memberPointRepository.targets = List.of(active(1L, 100L, 50, LocalDate.of(2026, 12, 31)));

        assertThatThrownBy(() -> useCase.execute(TARGET_DATE))
                .isInstanceOf(PointNotExpirableException.class);
        assertThat(memberPointRepository.saved).isEmpty();
        assertThat(historyRepository.added).isEmpty();
    }

    @Test
    void 更新件数が対象件数と合わなければ止まる() {
        memberPointRepository.targets = List.of(active(1L, 100L, 50, LocalDate.of(2026, 10, 18)));
        memberPointRepository.updatedCountOverride = 0;

        assertThatThrownBy(() -> useCase.execute(TARGET_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("失効の更新件数");
        assertThat(historyRepository.added).isEmpty();
    }

    @Test
    void 履歴の登録件数が対象件数と合わなければ止まる() {
        memberPointRepository.targets = List.of(active(1L, 100L, 50, LocalDate.of(2026, 10, 18)));
        historyRepository.insertedCountOverride = 0;

        assertThatThrownBy(() -> useCase.execute(TARGET_DATE))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("失効履歴の登録件数");
    }

    private static final class FakeMemberPointRepository implements MemberPointRepository {

        private List<MemberPoint> targets = List.of();
        private final List<MemberPoint> saved = new ArrayList<>();
        private Integer updatedCountOverride;

        @Override
        public List<MemberPoint> findExpirableAsOf(BusinessDate businessDate) {
            return targets;
        }

        @Override
        public int saveExpired(List<MemberPoint> expiredPoints) {
            saved.addAll(expiredPoints);
            return updatedCountOverride != null ? updatedCountOverride : expiredPoints.size();
        }
    }

    private static final class FakeHistoryRepository implements PointExpirationHistoryRepository {

        private final List<PointExpirationHistory> added = new ArrayList<>();
        private Integer insertedCountOverride;

        @Override
        public int addAll(List<PointExpirationHistory> histories) {
            added.addAll(histories);
            return insertedCountOverride != null ? insertedCountOverride : histories.size();
        }
    }
}
