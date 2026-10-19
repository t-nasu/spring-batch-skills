package com.example.orderimport.domain.model.order;

import com.example.orderimport.domain.model.BusinessDate;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 注文。取り込み処理における集約ルート。
 *
 * <p>このオブジェクトが生成できた時点で、単独で検証できる不変条件（各値の書式、
 * 数量が 1 以上であること）はすべて満たされている。顧客・商品がマスタに実在する
 * かどうかは他の集約を参照する必要があるため、ドメインサービス
 * {@link com.example.orderimport.domain.service.OrderValidationService} が受け持つ。
 *
 * @param orderNumber  注文番号（識別子）
 * @param businessDate 業務日付。日次売上の集計キーになる
 * @param customerCode 顧客コード
 * @param productCode  商品コード
 * @param quantity     数量
 * @param unitPrice    単価
 * @param orderedAt    注文日時。CSV に記録された値をそのまま保持する
 */
public record Order(
        OrderNumber orderNumber,
        BusinessDate businessDate,
        CustomerCode customerCode,
        ProductCode productCode,
        Quantity quantity,
        Money unitPrice,
        LocalDateTime orderedAt) {

    public Order {
        Objects.requireNonNull(orderNumber, "注文番号が空です");
        Objects.requireNonNull(businessDate, "業務日付が空です");
        Objects.requireNonNull(customerCode, "顧客コードが空です");
        Objects.requireNonNull(productCode, "商品コードが空です");
        Objects.requireNonNull(quantity, "数量が空です");
        Objects.requireNonNull(unitPrice, "単価が空です");
        Objects.requireNonNull(orderedAt, "注文日時が空です");
    }

    /** 注文金額（単価 × 数量）。 */
    public Money amount() {
        return unitPrice.multipliedBy(quantity.value());
    }
}
