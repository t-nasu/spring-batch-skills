package com.example.expirepoints.infrastructure;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** {@code member_point} 表へのアクセス。SQL は MemberPointMapper.xml にある。 */
@Mapper
public interface MemberPointMapper {

    /** 有効期限が業務日付より前で、かつ状態が ACTIVE の行を返す。 */
    List<MemberPointRecord> selectExpirable(@Param("targetDate") LocalDate targetDate);

    /**
     * 指定した行の状態を EXPIRED にする。すでに EXPIRED の行は更新しない。
     *
     * @return 更新した件数
     */
    int updateToExpired(@Param("ids") List<Long> ids);
}
