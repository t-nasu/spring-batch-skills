package com.example.monthlysales.domain.shared;

/**
 * 数量。
 *
 * <p>集計結果は返品が売上を上回ると負になりうるため、ここでは符号を制限しない。
 * 取引明細 1 件の数量が負でないことは {@code SalesTransaction} の生成時に確かめる。
 */
public record Quantity(long value) {

    public static Quantity of(long value) {
        return new Quantity(value);
    }

    public static Quantity zero() {
        return new Quantity(0L);
    }

    public Quantity add(Quantity other) {
        return new Quantity(value + other.value);
    }

    /** 符号を反転した数量。返品を差し引くときに使う。 */
    public Quantity negate() {
        return new Quantity(-value);
    }

    public boolean isNegative() {
        return value < 0;
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
