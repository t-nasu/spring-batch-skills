package com.example.pointbatch.domain.model.member;

import com.example.pointbatch.domain.exception.InvalidPointDataException;

/**
 * 会員 ID を表す値オブジェクト。
 */
public record MemberId(long value) {

    public MemberId {
        if (value <= 0L) {
            throw new InvalidPointDataException("会員 ID が不正です: " + value);
        }
    }

    public static MemberId of(long value) {
        return new MemberId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
