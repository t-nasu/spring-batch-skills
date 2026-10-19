package com.example.expirepoints.infrastructure;

import com.example.expirepoints.domain.BusinessDate;
import com.example.expirepoints.domain.MemberId;
import com.example.expirepoints.domain.MemberPoint;
import com.example.expirepoints.domain.MemberPointId;
import com.example.expirepoints.domain.MemberPointRepository;
import com.example.expirepoints.domain.PointAmount;
import com.example.expirepoints.domain.PointStatus;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Repository;

/** {@link MemberPointRepository} の MyBatis 実装。 */
@Repository
public class MyBatisMemberPointRepository implements MemberPointRepository {

    /**
     * 一度の SQL に載せる件数。業務上の区切りではなく、IN 句と {@code max_allowed_packet} が
     * 膨らみすぎないようにするための都合である。トランザクションは分かれない。
     */
    private static final int SQL_CHUNK_SIZE = 1_000;

    private final MemberPointMapper memberPointMapper;

    public MyBatisMemberPointRepository(MemberPointMapper memberPointMapper) {
        this.memberPointMapper = memberPointMapper;
    }

    @Override
    public List<MemberPoint> findExpirableAsOf(BusinessDate businessDate) {
        List<MemberPointRecord> records = memberPointMapper.selectExpirable(businessDate.value());
        List<MemberPoint> memberPoints = new ArrayList<>(records.size());
        for (MemberPointRecord record : records) {
            memberPoints.add(toDomain(record));
        }
        return memberPoints;
    }

    @Override
    public int saveExpired(List<MemberPoint> expiredPoints) {
        if (expiredPoints.isEmpty()) {
            return 0;
        }

        List<Long> ids = new ArrayList<>(expiredPoints.size());
        for (MemberPoint point : expiredPoints) {
            if (!point.isExpired()) {
                throw new IllegalArgumentException(
                        "失効していないポイントが渡された: id=" + point.id().value());
            }
            ids.add(point.id().value());
        }

        int updated = 0;
        for (int from = 0; from < ids.size(); from += SQL_CHUNK_SIZE) {
            int to = Math.min(from + SQL_CHUNK_SIZE, ids.size());
            updated += memberPointMapper.updateToExpired(ids.subList(from, to));
        }
        return updated;
    }

    private static MemberPoint toDomain(MemberPointRecord record) {
        return new MemberPoint(
                MemberPointId.of(record.getId()),
                MemberId.of(record.getMemberId()),
                PointAmount.of(record.getPoints()),
                record.getExpiresOn(),
                PointStatus.of(record.getStatus()));
    }
}
