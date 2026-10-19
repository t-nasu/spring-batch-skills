package com.example.pointbatch.application.expirepoints;

import com.example.pointbatch.domain.model.shared.BusinessDate;

/**
 * ポイント失効ジョブ(expirePointsJob)のユースケース。
 */
public interface ExpirePointsUseCase {

    /**
     * 業務日付時点で有効期限を過ぎたポイントを失効させ、失効履歴を登録する。
     *
     * @param businessDate 業務日付
     * @return 処理結果
     */
    ExpirePointsResult execute(BusinessDate businessDate);
}
