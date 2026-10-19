package com.example.salesbatch.domain.model;

/**
 * 店舗 ID を表す値オブジェクト。
 */
public record StoreId(String value) implements Comparable<StoreId> {

    public StoreId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("店舗 ID が未設定です");
        }
    }

    public static StoreId of(String value) {
        return new StoreId(value);
    }

    @Override
    public int compareTo(StoreId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
