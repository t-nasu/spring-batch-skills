package com.example.salesbatch.domain.model;

/**
 * 取引 ID を表す値オブジェクト。スキップしたデータのログ出力に使う。
 */
public record TransactionId(long value) implements Comparable<TransactionId> {

    public static TransactionId of(long value) {
        return new TransactionId(value);
    }

    @Override
    public int compareTo(TransactionId other) {
        return Long.compare(value, other.value);
    }

    @Override
    public String toString() {
        return Long.toString(value);
    }
}
