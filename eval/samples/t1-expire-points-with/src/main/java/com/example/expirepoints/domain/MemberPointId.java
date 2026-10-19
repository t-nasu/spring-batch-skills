package com.example.expirepoints.domain;

/** 会員ポイントの ID。 */
public record MemberPointId(long value) {

    public MemberPointId {
        if (value <= 0) {
            throw new InvalidMemberPointException("会員ポイント ID が不正である: " + value);
        }
    }

    public static MemberPointId of(Long value) {
        if (value == null) {
            throw new InvalidMemberPointException("会員ポイント ID が空である");
        }
        return new MemberPointId(value);
    }
}
