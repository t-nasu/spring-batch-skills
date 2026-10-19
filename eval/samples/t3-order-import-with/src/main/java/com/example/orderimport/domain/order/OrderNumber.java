package com.example.orderimport.domain.order;

/**
 * 注文番号。注文の同一性を決める。
 */
public record OrderNumber(String value) {

    public OrderNumber {
        if (value == null || value.isBlank()) {
            throw new InvalidOrderException("注文番号は必須である");
        }
        value = value.trim();
    }

    public static OrderNumber of(String value) {
        return new OrderNumber(value);
    }

    @Override
    public String toString() {
        return value;
    }
}
