package com.example.expirepoints.domain;

/** ポイント数。失効の対象になるのは 1 以上のポイントだけである。 */
public record PointAmount(int value) {

    public PointAmount {
        if (value <= 0) {
            throw new InvalidMemberPointException("ポイント数が不正である: " + value);
        }
    }

    public static PointAmount of(Integer value) {
        if (value == null) {
            throw new InvalidMemberPointException("ポイント数が空である");
        }
        return new PointAmount(value);
    }
}
