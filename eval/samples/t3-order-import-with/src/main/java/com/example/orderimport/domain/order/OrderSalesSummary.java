package com.example.orderimport.domain.order;

import com.example.orderimport.domain.shared.Money;

import java.util.Objects;

/**
 * ある業務日付に属する注文の件数と合計金額。日次売上の元になる。
 */
public record OrderSalesSummary(long orderCount, Money totalAmount) {

    public static final OrderSalesSummary EMPTY = new OrderSalesSummary(0L, Money.ZERO);

    public OrderSalesSummary {
        Objects.requireNonNull(totalAmount, "合計金額は必須である");
        if (orderCount < 0) {
            throw new IllegalArgumentException("件数に負の値は使えない: " + orderCount);
        }
    }
}
