package com.example.pointbatch.infrastructure.datasource.mybatis;

import com.example.pointbatch.domain.exception.InvalidPointDataException;
import com.example.pointbatch.domain.exception.PointUpdateConflictException;
import com.example.pointbatch.domain.model.member.MemberId;
import com.example.pointbatch.domain.model.point.MemberPoint;
import com.example.pointbatch.domain.model.point.MemberPointId;
import com.example.pointbatch.domain.model.point.PointAmount;
import com.example.pointbatch.domain.model.point.PointStatus;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import com.example.pointbatch.domain.repository.MemberPointRepository;
import com.example.pointbatch.infrastructure.datasource.mybatis.mapper.MemberPointMapper;
import com.example.pointbatch.infrastructure.datasource.mybatis.record.MemberPointRecord;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

/**
 * {@link MemberPointRepository} の MyBatis 実装。
 */
@Repository
public class MyBatisMemberPointRepository implements MemberPointRepository {

    private final MemberPointMapper memberPointMapper;

    public MyBatisMemberPointRepository(MemberPointMapper memberPointMapper) {
        this.memberPointMapper = memberPointMapper;
    }

    @Override
    public List<MemberPoint> findExpirationTargets(
            BusinessDate businessDate, Optional<MemberPointId> exclusiveStartId, int limit) {

        Long startId = exclusiveStartId.map(MemberPointId::value).orElse(null);
        List<MemberPointRecord> records =
                memberPointMapper.selectExpirationTargets(businessDate.value(), startId, limit);
        return records.stream().map(MyBatisMemberPointRepository::toDomain).toList();
    }

    @Override
    public void saveExpired(List<MemberPoint> expiredPoints) {
        if (expiredPoints.isEmpty()) {
            return;
        }
        List<Long> ids = expiredPoints.stream().map(point -> point.id().value()).toList();
        int updated = memberPointMapper.updateToExpired(ids);
        if (updated != ids.size()) {
            throw new PointUpdateConflictException(
                    "失効対象の更新件数が一致しません(他処理による更新の可能性があります): expected=%d, actual=%d, memberPointIds=%s"
                            .formatted(ids.size(), updated, ids));
        }
    }

    private static MemberPoint toDomain(MemberPointRecord record) {
        if (record.getId() == null) {
            throw new InvalidPointDataException("member_point.id が NULL です");
        }
        if (record.getMemberId() == null) {
            throw new InvalidPointDataException(
                    "member_point.member_id が NULL です: id=" + record.getId());
        }
        if (record.getAmount() == null) {
            throw new InvalidPointDataException(
                    "member_point.amount が NULL です: id=" + record.getId());
        }
        return MemberPoint.reconstruct(
                MemberPointId.of(record.getId()),
                MemberId.of(record.getMemberId()),
                PointAmount.of(record.getAmount()),
                record.getExpiresOn(),
                PointStatus.of(record.getStatus()));
    }
}
