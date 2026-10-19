package com.example.monthlysales.domain.shared;

/** 店舗を識別する値。 */
public record StoreId(String value) {

    public StoreId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("店舗 ID が空である");
        }
    }

    public static StoreId of(String value) {
        return new StoreId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
