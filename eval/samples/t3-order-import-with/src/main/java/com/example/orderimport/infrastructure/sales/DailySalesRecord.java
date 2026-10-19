package com.example.orderimport.infrastructure.sales;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * daily_sales 表の 1 行。
 */
public class DailySalesRecord {

    private LocalDate businessDate;
    private long orderCount;
    private BigDecimal totalAmount;

    public LocalDate getBusinessDate() {
        return businessDate;
    }

    public void setBusinessDate(LocalDate businessDate) {
        this.businessDate = businessDate;
    }

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
