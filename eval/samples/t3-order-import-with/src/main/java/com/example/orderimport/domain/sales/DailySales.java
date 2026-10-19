package com.example.orderimport.domain.sales;

import com.example.orderimport.domain.shared.BusinessDate;
import com.example.orderimport.domain.shared.Money;

import java.util.Objects;

/**
 * 日次売上。業務日付ごとに一つだけ存在する。
 */
public record DailySales(BusinessDate businessDate, long orderCount, Money totalAmount) {

    public DailySales {
        Objects.requireNonNull(businessDate, "業務日付は必須である");
        Objects.requireNonNull(totalAmount, "合計金額は必須である");
        if (orderCount < 0) {
            throw new IllegalArgumentException("件数に負の値は使えない: " + orderCount);
        }
    }

    public static DailySales of(BusinessDate businessDate, long orderCount, Money totalAmount) {
        return new DailySales(businessDate, orderCount, totalAmount);
    }
}
