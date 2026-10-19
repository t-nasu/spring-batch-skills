package com.example.expirepoints.infrastructure;

import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

/** {@code point_expiration_history} 表へのアクセス。SQL は PointExpirationHistoryMapper.xml にある。 */
@Mapper
public interface PointExpirationHistoryMapper {

    /**
     * 失効履歴をまとめて登録する。
     *
     * @return 登録した件数
     */
    int insertAll(@Param("records") List<PointExpirationHistoryRecord> records);
}
