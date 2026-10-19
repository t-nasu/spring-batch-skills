package com.example.pointbatch.domain.exception;

/**
 * 入力データがドメインの制約を満たしていないことを表す例外。
 *
 * <p>仕様「不正データがあったとき: ジョブを止める」に従い、この例外は捕捉せずジョブを異常終了させる。
 */
public class InvalidPointDataException extends DomainException {

    public InvalidPointDataException(String message) {
        super(message);
    }
}
