package com.example.pointbatch.domain.repository;

import com.example.pointbatch.domain.exception.PointUpdateConflictException;
import com.example.pointbatch.domain.model.point.MemberPoint;
import com.example.pointbatch.domain.model.point.MemberPointId;
import com.example.pointbatch.domain.model.shared.BusinessDate;
import java.util.List;
import java.util.Optional;

/**
 * 会員ポイントのリポジトリ。
 */
public interface MemberPointRepository {

    /**
     * 失効対象(有効かつ業務日付時点で期限切れ)のポイントを ID 昇順で取得する。
     *
     * <p>件数が多くなりうるため、ID によるキーセットページングで分割取得する。
     *
     * @param businessDate     業務日付
     * @param exclusiveStartId この ID より大きいものを取得する。空の場合は先頭から。
     * @param limit            最大取得件数
     * @return 失効対象のポイント(ID 昇順)
     */
    List<MemberPoint> findExpirationTargets(
            BusinessDate businessDate, Optional<MemberPointId> exclusiveStartId, int limit);

    /**
     * 失効させたポイントの状態を永続化する。
     *
     * <p>読み取り後に他処理が状態を変更していた場合は更新件数が不足するため、
     * {@link PointUpdateConflictException} を送出する。
     *
     * @param expiredPoints 失効済み状態になったポイント
     * @throws PointUpdateConflictException 更新できなかったポイントがある場合
     */
    void saveExpired(List<MemberPoint> expiredPoints);
}
