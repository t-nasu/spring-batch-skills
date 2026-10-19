package com.example.orderimport.domain.product;

/**
 * 商品コード。
 */
public record ProductCode(String value) {

    public ProductCode {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("商品コードは必須である");
        }
        value = value.trim();
    }

    public static ProductCode of(String value) {
        return new ProductCode(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
