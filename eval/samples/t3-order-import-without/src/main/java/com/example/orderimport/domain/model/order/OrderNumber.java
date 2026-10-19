package com.example.orderimport.domain.model.order;

/**
 * 注文番号。注文集約の識別子。
 */
public record OrderNumber(String value) {

    private static final int MAX_LENGTH = 32;

    public OrderNumber {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("注文番号が空です");
        }
        value = value.trim();
        if (value.length() > MAX_LENGTH) {
            throw new IllegalArgumentException(
                    "注文番号は " + MAX_LENGTH + " 文字以内で指定してください: " + value);
        }
    }

    public static OrderNumber of(String value) {
        return new OrderNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
