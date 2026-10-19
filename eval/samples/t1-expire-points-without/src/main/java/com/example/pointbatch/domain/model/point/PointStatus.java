package com.example.pointbatch.domain.model.point;

import com.example.pointbatch.domain.exception.InvalidPointDataException;
import java.util.Arrays;

/**
 * 会員ポイントの状態。
 */
public enum PointStatus {

    /** 有効。失効処理の対象となりうる状態。 */
    ACTIVE,

    /** 使用済み。 */
    USED,

    /** 失効済み。 */
    EXPIRED;

    /**
     * DB 上の文字列から列挙子を復元する。
     *
     * @throws InvalidPointDataException 未知の状態値の場合
     */
    public static PointStatus of(String value) {
        return Arrays.stream(values())
                .filter(status -> status.name().equals(value))
                .findFirst()
                .orElseThrow(() -> new InvalidPointDataException("ポイント状態が不正です: " + value));
    }
}
