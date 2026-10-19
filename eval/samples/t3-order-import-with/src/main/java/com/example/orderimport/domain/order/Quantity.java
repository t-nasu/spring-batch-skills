package com.example.orderimport.domain.order;

/**
 * 注文数量。1 以上でなければならない（仕様 4-1）。
 */
public record Quantity(int value) {

    public Quantity {
        if (value < 1) {
            throw new InvalidOrderException("数量は 1 以上である必要がある: " + value);
        }
    }

    public static Quantity of(int value) {
        return new Quantity(value);
    }

    @Override
    public String toString() {
        return String.valueOf(value);
    }
}
