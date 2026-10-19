package com.example.orderimport.domain.order;

import com.example.orderimport.domain.customer.CustomerCode;
import com.example.orderimport.domain.product.ProductCode;
import com.example.orderimport.domain.shared.BusinessDate;
import com.example.orderimport.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    @DisplayName("金額は単価かける数量である")
    void amountIsUnitPriceTimesQuantity() {
        Order order = order(3, "1200.50");

        assertThat(order.amount()).isEqualTo(Money.of(new BigDecimal("3601.50")));
    }

    @Test
    @DisplayName("業務日付は注文日時の日付である")
    void orderDateIsDatePartOfOrderedAt() {
        Order order = order(1, "100");

        assertThat(order.orderDate()).isEqualTo(BusinessDate.of(LocalDate.of(2026, 10, 18)));
    }

    @Test
    @DisplayName("数量が 0 以下なら取り込めない")
    void quantityBelowOneIsRejected() {
        assertThatThrownBy(() -> Quantity.of(0))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("1 以上");
    }

    @Test
    @DisplayName("注文番号が空なら取り込めない")
    void blankOrderNumberIsRejected() {
        assertThatThrownBy(() -> OrderNumber.of(" "))
                .isInstanceOf(InvalidOrderException.class);
    }

    private static Order order(int quantity, String unitPrice) {
        return new Order(
                OrderNumber.of("ORD-001"),
                CustomerCode.of("C001"),
                ProductCode.of("P001"),
                Quantity.of(quantity),
                Money.of(new BigDecimal(unitPrice)),
                LocalDateTime.of(2026, 10, 18, 23, 59, 59));
    }
}
