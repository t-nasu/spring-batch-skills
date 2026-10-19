package com.example.expirepoints.domain;

import java.util.List;

/** 会員ポイントの入出力。 */
public interface MemberPointRepository {

    /**
     * 業務日付の時点で失効させるべき会員ポイントをすべて返す。
     *
     * @return 対象が無ければ空のリスト
     */
    List<MemberPoint> findExpirableAsOf(BusinessDate businessDate);

    /**
     * 失効させた会員ポイントの状態を書き戻す。
     *
     * @param expiredPoints すべて {@link PointStatus#EXPIRED} であること
     * @return 更新した件数
     */
    int saveExpired(List<MemberPoint> expiredPoints);
}
