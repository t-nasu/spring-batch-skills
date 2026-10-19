package com.example.pointbatch.domain.model.point;

import com.example.pointbatch.domain.exception.InvalidPointDataException;

/**
 * 会員ポイント(member_point)の識別子を表す値オブジェクト。
 */
public record MemberPointId(long value) {

    public MemberPointId {
        if (value <= 0L) {
            throw new InvalidPointDataException("会員ポイント ID が不正です: " + value);
        }
    }

    public static MemberPointId of(long value) {
        return new MemberPointId(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
