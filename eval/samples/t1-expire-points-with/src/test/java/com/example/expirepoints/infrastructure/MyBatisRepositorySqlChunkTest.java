package com.example.expirepoints.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.expirepoints.domain.BusinessDate;
import com.example.expirepoints.domain.MemberId;
import com.example.expirepoints.domain.MemberPoint;
import com.example.expirepoints.domain.MemberPointId;
import com.example.expirepoints.domain.PointAmount;
import com.example.expirepoints.domain.PointExpirationHistory;
import com.example.expirepoints.domain.PointStatus;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * 最大 20,000 件を 1,000 件ずつの SQL に分けて流すところを確かめる。結合テストでは件数が
 * 少なく、この分割を通らないため。
 */
class MyBatisRepositorySqlChunkTest {

    private static final BusinessDate TARGET_DATE = BusinessDate.parse("20261019");
    private static final LocalDate EXPIRED_ON = LocalDate.of(2026, 10, 18);

    private static List<MemberPoint> expiredPoints(int count) {
        List<MemberPoint> points = new ArrayList<>(count);
        for (int i = 1; i <= count; i++) {
            points.add(
                    new MemberPoint(
                                    new MemberPointId(i),
                                    new MemberId(i),
                                    new PointAmount(10),
                                    EXPIRED_ON,
                                    PointStatus.ACTIVE)
                            .expire(TARGET_DATE));
        }
        return points;
    }

    @Test
    void 会員ポイントの更新を_1000_件ずつに分ける() {
        var mapper = new RecordingMemberPointMapper();
        var repository = new MyBatisMemberPointRepository(mapper);

        int updated = repository.saveExpired(expiredPoints(2_500));

        assertThat(mapper.batchSizes).containsExactly(1_000, 1_000, 500);
        assertThat(updated).isEqualTo(2_500);
        assertThat(mapper.allIds).hasSize(2_500).doesNotHaveDuplicates();
    }

    @Test
    void 対象が無ければ_SQL_を流さない() {
        var mapper = new RecordingMemberPointMapper();
        var repository = new MyBatisMemberPointRepository(mapper);

        assertThat(repository.saveExpired(List.of())).isZero();
        assertThat(mapper.batchSizes).isEmpty();
    }

    @Test
    void 失効していないポイントを渡されたら弾く() {
        var mapper = new RecordingMemberPointMapper();
        var repository = new MyBatisMemberPointRepository(mapper);
        var active =
                new MemberPoint(
                        new MemberPointId(1L),
                        new MemberId(1L),
                        new PointAmount(10),
                        EXPIRED_ON,
                        PointStatus.ACTIVE);

        assertThatThrownBy(() -> repository.saveExpired(List.of(active)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(mapper.batchSizes).isEmpty();
    }

    @Test
    void 失効履歴の登録を_1000_件ずつに分ける() {
        var mapper = new RecordingHistoryMapper();
        var repository = new MyBatisPointExpirationHistoryRepository(mapper);
        List<PointExpirationHistory> histories =
                expiredPoints(2_500).stream().map(p -> PointExpirationHistory.of(p, TARGET_DATE)).toList();

        int inserted = repository.addAll(histories);

        assertThat(mapper.batchSizes).containsExactly(1_000, 1_000, 500);
        assertThat(inserted).isEqualTo(2_500);
        assertThat(mapper.allRecords)
                .hasSize(2_500)
                .allMatch(r -> r.getExpiredOn().equals(LocalDate.of(2026, 10, 19)));
    }

    @Test
    void 履歴が無ければ_SQL_を流さない() {
        var mapper = new RecordingHistoryMapper();
        var repository = new MyBatisPointExpirationHistoryRepository(mapper);

        assertThat(repository.addAll(List.of())).isZero();
        assertThat(mapper.batchSizes).isEmpty();
    }

    private static final class RecordingMemberPointMapper implements MemberPointMapper {

        private final List<Integer> batchSizes = new ArrayList<>();
        private final List<Long> allIds = new ArrayList<>();

        @Override
        public List<MemberPointRecord> selectExpirable(LocalDate targetDate) {
            throw new UnsupportedOperationException();
        }

        @Override
        public int updateToExpired(List<Long> ids) {
            batchSizes.add(ids.size());
            allIds.addAll(ids);
            return ids.size();
        }
    }

    private static final class RecordingHistoryMapper implements PointExpirationHistoryMapper {

        private final List<Integer> batchSizes = new ArrayList<>();
        private final List<PointExpirationHistoryRecord> allRecords = new ArrayList<>();

        @Override
        public int insertAll(List<PointExpirationHistoryRecord> records) {
            batchSizes.add(records.size());
            allRecords.addAll(records);
            return records.size();
        }
    }
}
