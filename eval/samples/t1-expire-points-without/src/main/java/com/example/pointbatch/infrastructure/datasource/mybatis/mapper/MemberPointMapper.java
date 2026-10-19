package com.example.pointbatch.infrastructure.datasource.mybatis.mapper;

import com.example.pointbatch.infrastructure.datasource.mybatis.record.MemberPointRecord;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * member_point テーブルへのアクセス。
 */
@Mapper
public interface MemberPointMapper {

    /**
     * 失効対象(status = 'ACTIVE' かつ expires_on &lt; targetDate)を ID 昇順で取得する。
     *
     * @param targetDate       業務日付
     * @param exclusiveStartId この ID より大きい行を取得する。null の場合は先頭から。
     * @param limit            最大取得件数
     */
    List<MemberPointRecord> selectExpirationTargets(
            @Param("targetDate") LocalDate targetDate,
            @Param("exclusiveStartId") Long exclusiveStartId,
            @Param("limit") int limit);

    /**
     * 指定した ID のポイントのうち、まだ有効なものを失効済みに更新する。
     *
     * @param ids 更新対象の会員ポイント ID(1 件以上)
     * @return 更新された行数
     */
    int updateToExpired(@Param("ids") List<Long> ids);
}
