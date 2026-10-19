package com.example.salesbatch.domain.model;

/**
 * 商品 ID を表す値オブジェクト。
 */
public record ProductId(String value) implements Comparable<ProductId> {

    public ProductId {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("商品 ID が未設定です");
        }
    }

    public static ProductId of(String value) {
        return new ProductId(value);
    }

    @Override
    public int compareTo(ProductId other) {
        return value.compareTo(other.value);
    }

    @Override
    public String toString() {
        return value;
    }
}
