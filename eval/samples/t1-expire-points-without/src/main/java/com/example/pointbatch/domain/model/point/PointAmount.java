package com.example.pointbatch.domain.model.point;

import com.example.pointbatch.domain.exception.InvalidPointDataException;

/**
 * ポイント数を表す値オブジェクト。
 *
 * <p>負値は不正データとして扱う。0 は「残高を使い切った」状態として正常に扱う。
 */
public record PointAmount(int value) {

    public PointAmount {
        if (value < 0) {
            throw new InvalidPointDataException("ポイント数が不正です: " + value);
        }
    }

    public static PointAmount of(int value) {
        return new PointAmount(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
