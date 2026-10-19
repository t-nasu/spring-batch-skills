package com.example.expirepoints.domain;

/** 会員ポイントの状態。 */
public enum PointStatus {
    /** 有効。失効の対象になりうる。 */
    ACTIVE,
    /** 失効済み。 */
    EXPIRED;

    public static PointStatus of(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidMemberPointException("ポイント状態が空である");
        }
        try {
            return valueOf(value);
        } catch (IllegalArgumentException e) {
            throw new InvalidMemberPointException("ポイント状態が不正である: " + value, e);
        }
    }
}
