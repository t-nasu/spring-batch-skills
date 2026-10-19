package com.example.expirepoints.domain;

/**
 * 会員ポイントのデータが業務上ありえない値だったことを表す。
 *
 * <p>仕様「不正データがあったとき: ジョブを止める」に従い、この例外は捕まえずに送出し、
 * トランザクションを巻き戻してジョブを異常終了させる。
 */
public class InvalidMemberPointException extends RuntimeException {

    public InvalidMemberPointException(String message) {
        super(message);
    }

    public InvalidMemberPointException(String message, Throwable cause) {
        super(message, cause);
    }
}
