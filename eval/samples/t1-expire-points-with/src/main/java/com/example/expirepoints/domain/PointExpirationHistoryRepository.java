package com.example.expirepoints.domain;

import java.util.List;

/** 失効履歴の出力。 */
public interface PointExpirationHistoryRepository {

    /**
     * 失効履歴を登録する。
     *
     * @return 登録した件数
     */
    int addAll(List<PointExpirationHistory> histories);
}
