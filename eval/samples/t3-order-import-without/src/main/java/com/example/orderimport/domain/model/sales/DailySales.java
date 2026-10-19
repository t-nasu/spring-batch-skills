package com.example.orderimport.domain.model.sales;

import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Money;
import com.example.orderimport.domain.model.order.ProductCode;
import java.util.Objects;

/**
 * 日次売上。粒度は「業務日付 × 商品」。
 *
 * @param businessDate  業務日付
 * @param productCode   商品コード
 * @param orderCount    注文件数
 * @param totalQuantity 数量合計
 * @param totalAmount   売上金額合計
 */
public record DailySales(
        BusinessDate businessDate,
        ProductCode productCode,
        int orderCount,
        long totalQuantity,
        Money totalAmount) {

    public DailySales {
        Objects.requireNonNull(businessDate, "業務日付が空です");
        Objects.requireNonNull(productCode, "商品コードが空です");
        Objects.requireNonNull(totalAmount, "売上金額合計が空です");
        if (orderCount < 0) {
            throw new IllegalArgumentException("注文件数は 0 以上で指定してください: " + orderCount);
        }
        if (totalQuantity < 0) {
            throw new IllegalArgumentException("数量合計は 0 以上で指定してください: " + totalQuantity);
        }
    }
}
