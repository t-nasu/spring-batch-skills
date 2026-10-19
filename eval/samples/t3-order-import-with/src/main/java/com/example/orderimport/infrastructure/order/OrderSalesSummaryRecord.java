package com.example.orderimport.infrastructure.order;

import java.math.BigDecimal;

/**
 * orders 表を業務日付で集計した結果の 1 行。
 */
public class OrderSalesSummaryRecord {

    private long orderCount;
    private BigDecimal totalAmount;

    public long getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(long orderCount) {
        this.orderCount = orderCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }
}
