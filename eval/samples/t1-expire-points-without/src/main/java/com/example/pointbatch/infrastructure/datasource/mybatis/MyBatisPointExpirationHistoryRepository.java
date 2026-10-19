package com.example.pointbatch.infrastructure.datasource.mybatis;

import com.example.pointbatch.domain.model.point.PointExpirationHistory;
import com.example.pointbatch.domain.repository.PointExpirationHistoryRepository;
import com.example.pointbatch.infrastructure.datasource.mybatis.mapper.PointExpirationHistoryMapper;
import com.example.pointbatch.infrastructure.datasource.mybatis.record.PointExpirationHistoryRecord;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * {@link PointExpirationHistoryRepository} の MyBatis 実装。
 */
@Repository
public class MyBatisPointExpirationHistoryRepository implements PointExpirationHistoryRepository {

    private final PointExpirationHistoryMapper pointExpirationHistoryMapper;

    public MyBatisPointExpirationHistoryRepository(
            PointExpirationHistoryMapper pointExpirationHistoryMapper) {
        this.pointExpirationHistoryMapper = pointExpirationHistoryMapper;
    }

    @Override
    public void saveAll(List<PointExpirationHistory> histories) {
        if (histories.isEmpty()) {
            return;
        }
        List<PointExpirationHistoryRecord> records =
                histories.stream().map(MyBatisPointExpirationHistoryRepository::toRecord).toList();
        pointExpirationHistoryMapper.insertAll(records);
    }

    private static PointExpirationHistoryRecord toRecord(PointExpirationHistory history) {
        return new PointExpirationHistoryRecord(
                history.memberPointId().value(),
                history.memberId().value(),
                history.amount().value(),
                history.expiredOn());
    }
}
