package com.example.orderimport.infrastructure.order;

import com.example.orderimport.domain.order.InvalidOrderException;
import com.example.orderimport.domain.order.Order;
import com.example.orderimport.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.infrastructure.item.file.transform.DefaultFieldSet;
import org.springframework.batch.infrastructure.item.file.transform.FieldSet;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderCsvItemReaderTest {

    @Test
    @DisplayName("CSV の 1 行を注文に変換する")
    void convertsCsvLineToOrder() {
        Order order = OrderCsvItemReader.toOrder(
                fieldSet("ORD-001", "C001", "P001", "3", "1200.50", "2026-10-18 09:30:00"));

        assertThat(order.orderNumber().value()).isEqualTo("ORD-001");
        assertThat(order.customerCode().value()).isEqualTo("C001");
        assertThat(order.productCode().value()).isEqualTo("P001");
        assertThat(order.quantity().value()).isEqualTo(3);
        assertThat(order.unitPrice()).isEqualTo(Money.of(new BigDecimal("1200.50")));
        assertThat(order.orderedAt()).isEqualTo(LocalDateTime.of(2026, 10, 18, 9, 30, 0));
    }

    @Test
    @DisplayName("数量が 1 未満の行は読み飛ばせる例外にする")
    void turnsInvalidQuantityIntoSkippableException() {
        assertThatThrownBy(() -> OrderCsvItemReader.toOrder(
                fieldSet("ORD-002", "C001", "P001", "0", "100", "2026-10-18 09:30:00")))
                .isInstanceOf(InvalidOrderException.class);
    }

    @Test
    @DisplayName("注文日時が読めない行は読み飛ばせる例外にする")
    void turnsUnparsableTimestampIntoSkippableException() {
        assertThatThrownBy(() -> OrderCsvItemReader.toOrder(
                fieldSet("ORD-003", "C001", "P001", "1", "100", "2026/10/18 09:30")))
                .isInstanceOf(InvalidOrderException.class)
                .hasMessageContaining("注文として読めない行である");
    }

    @Test
    @DisplayName("数量が数字でない行は読み飛ばせる例外にする")
    void turnsNonNumericQuantityIntoSkippableException() {
        assertThatThrownBy(() -> OrderCsvItemReader.toOrder(
                fieldSet("ORD-004", "C001", "P001", "x", "100", "2026-10-18 09:30:00")))
                .isInstanceOf(InvalidOrderException.class);
    }

    private static FieldSet fieldSet(String... values) {
        return new DefaultFieldSet(values, OrderCsvItemReader.COLUMNS);
    }
}
