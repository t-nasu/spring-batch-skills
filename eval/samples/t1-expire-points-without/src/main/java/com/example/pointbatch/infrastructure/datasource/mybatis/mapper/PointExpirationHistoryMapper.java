package com.example.pointbatch.infrastructure.datasource.mybatis.mapper;

import com.example.pointbatch.infrastructure.datasource.mybatis.record.PointExpirationHistoryRecord;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/**
 * point_expiration_history テーブルへのアクセス。
 */
@Mapper
public interface PointExpirationHistoryMapper {

    /**
     * 失効履歴をまとめて登録する。
     *
     * @param records 登録する行(1 件以上)
     * @return 登録された行数
     */
    int insertAll(@Param("records") List<PointExpirationHistoryRecord> records);
}
