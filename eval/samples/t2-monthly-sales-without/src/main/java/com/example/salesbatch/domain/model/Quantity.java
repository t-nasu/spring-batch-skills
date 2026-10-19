package com.example.salesbatch.domain.model;

/**
 * 数量を表す値オブジェクト。
 *
 * <p>{@link Money} と同様、返品を差し引いた合計は負になりうるためこの型は負値を許容する。
 * 個々の取引明細の数量が 1 以上であることは {@link RawSalesTransaction} の検証で担保する。</p>
 */
public record Quantity(int value) implements Comparable<Quantity> {

    public static final Quantity ZERO = new Quantity(0);

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    public Quantity add(Quantity other) {
        return new Quantity(Math.addExact(value, other.value));
    }

    public Quantity subtract(Quantity other) {
        return new Quantity(Math.subtractExact(value, other.value));
    }

    /** 符号（+1 / -1）を掛けた数量。 */
    public Quantity multipliedBy(int factor) {
        return new Quantity(Math.multiplyExact(value, factor));
    }

    @Override
    public int compareTo(Quantity other) {
        return Integer.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Integer.toString(value);
    }
}
