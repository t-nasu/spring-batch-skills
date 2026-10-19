package com.example.orderimport.domain.model.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderimport.domain.model.BusinessDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class OrderTest {

    @Test
    @DisplayName("注文金額は単価 × 数量")
    void 注文金額を計算する() {
        Order order = new Order(
                OrderNumber.of("ORD-0001"),
                BusinessDate.parse("20260118"),
                CustomerCode.of("C001"),
                ProductCode.of("P001"),
                Quantity.of(3),
                Money.parse("1980.50"),
                LocalDateTime.of(2026, 1, 18, 10, 30, 0));

        assertThat(order.amount()).isEqualTo(Money.parse("5941.50"));
    }
}
