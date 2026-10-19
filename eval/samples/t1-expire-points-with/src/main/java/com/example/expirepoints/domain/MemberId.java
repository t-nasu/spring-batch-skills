package com.example.expirepoints.domain;

/** 会員 ID。 */
public record MemberId(long value) {

    public MemberId {
        if (value <= 0) {
            throw new InvalidMemberPointException("会員 ID が不正である: " + value);
        }
    }

    public static MemberId of(Long value) {
        if (value == null) {
            throw new InvalidMemberPointException("会員 ID が空である");
        }
        return new MemberId(value);
    }
}
