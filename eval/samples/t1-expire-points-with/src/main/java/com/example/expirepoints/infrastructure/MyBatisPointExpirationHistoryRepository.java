package com.example.expirepoints.infrastructure;

import com.example.expirepoints.domain.PointExpirationHistory;
import com.example.expirepoints.domain.PointExpirationHistoryRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/** {@link PointExpirationHistoryRepository} の MyBatis 実装。 */
@Repository
public class MyBatisPointExpirationHistoryRepository implements PointExpirationHistoryRepository {

    /** 一度の INSERT に載せる件数。{@link MyBatisMemberPointRepository} と同じ理由による。 */
    private static final int SQL_CHUNK_SIZE = 1_000;

    private final PointExpirationHistoryMapper pointExpirationHistoryMapper;

    public MyBatisPointExpirationHistoryRepository(
            PointExpirationHistoryMapper pointExpirationHistoryMapper) {
        this.pointExpirationHistoryMapper = pointExpirationHistoryMapper;
    }

    @Override
    public int addAll(List<PointExpirationHistory> histories) {
        if (histories.isEmpty()) {
            return 0;
        }

        int inserted = 0;
        for (int from = 0; from < histories.size(); from += SQL_CHUNK_SIZE) {
            int to = Math.min(from + SQL_CHUNK_SIZE, histories.size());
            inserted += pointExpirationHistoryMapper.insertAll(toRecords(histories.subList(from, to)));
        }
        return inserted;
    }

    private static List<PointExpirationHistoryRecord> toRecords(
            List<PointExpirationHistory> histories) {
        return histories.stream().map(MyBatisPointExpirationHistoryRepository::toRecord).toList();
    }

    private static PointExpirationHistoryRecord toRecord(PointExpirationHistory history) {
        PointExpirationHistoryRecord record = new PointExpirationHistoryRecord();
        record.setMemberPointId(history.memberPointId().value());
        record.setMemberId(history.memberId().value());
        record.setPoints(history.amount().value());
        record.setExpiredOn(history.expiredOn());
        return record;
    }
}
