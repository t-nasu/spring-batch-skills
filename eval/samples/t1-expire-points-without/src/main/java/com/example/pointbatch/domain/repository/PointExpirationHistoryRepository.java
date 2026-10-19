package com.example.pointbatch.domain.repository;

import com.example.pointbatch.domain.model.point.PointExpirationHistory;
import java.util.List;

/**
 * ポイント失効履歴のリポジトリ。
 */
public interface PointExpirationHistoryRepository {

    /**
     * 失効履歴をまとめて登録する。
     *
     * @param histories 登録する失効履歴
     */
    void saveAll(List<PointExpirationHistory> histories);
}
