package com.example.pointbatch.domain.exception;

/**
 * 失効対象として読み取ったポイントが、更新時点で既に他処理によって変更されていたことを表す例外。
 *
 * <p>本バッチは再試行しない方針のため、この例外が発生した場合はジョブを異常終了させる。
 * 再実行すれば変更済みのレコードは抽出条件から外れるため、最初からやり直して問題ない。
 */
public class PointUpdateConflictException extends DomainException {

    public PointUpdateConflictException(String message) {
        super(message);
    }
}
