package com.example.orderimport.domain.order;

import com.example.orderimport.domain.customer.CustomerCode;
import com.example.orderimport.domain.product.ProductCode;
import com.example.orderimport.domain.shared.BusinessDate;
import com.example.orderimport.domain.shared.Money;

import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 注文。取引先から届く CSV の 1 行に対応する。
 */
public record Order(
        OrderNumber orderNumber,
        CustomerCode customerCode,
        ProductCode productCode,
        Quantity quantity,
        Money unitPrice,
        LocalDateTime orderedAt) {

    public Order {
        Objects.requireNonNull(orderNumber, "注文番号は必須である");
        Objects.requireNonNull(customerCode, "顧客コードは必須である");
        Objects.requireNonNull(productCode, "商品コードは必須である");
        Objects.requireNonNull(quantity, "数量は必須である");
        Objects.requireNonNull(unitPrice, "単価は必須である");
        Objects.requireNonNull(orderedAt, "注文日時は必須である");
    }

    /**
     * この注文の金額（単価 × 数量）。
     */
    public Money amount() {
        return unitPrice.times(quantity.value());
    }

    /**
     * この注文が属する業務日付。日次売上の集計単位になる。
     */
    public BusinessDate orderDate() {
        return BusinessDate.of(orderedAt.toLocalDate());
    }
}
