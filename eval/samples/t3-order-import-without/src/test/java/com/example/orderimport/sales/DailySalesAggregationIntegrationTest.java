package com.example.orderimport.sales;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.orderimport.application.sales.DailySalesAggregationService;
import com.example.orderimport.application.sales.DailySalesAggregationService.AggregationResult;
import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Money;
import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.domain.model.order.OrderNumber;
import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.model.order.Quantity;
import com.example.orderimport.domain.model.order.CustomerCode;
import com.example.orderimport.domain.model.sales.DailySales;
import com.example.orderimport.domain.repository.DailySalesRepository;
import com.example.orderimport.domain.repository.OrderRepository;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

/**
 * 日次売上の集計そのものの検証。
 *
 * <p>とくに「何度実行しても同じ結果になる」ことを確かめる。取り込みを途中から
 * 再開したあとに集計が走る以上、冪等でなければ売上が二重計上されてしまう。
 */
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties =
        "spring.datasource.url=jdbc:h2:mem:order_import_sales;MODE=MySQL;DB_CLOSE_DELAY=-1")
class DailySalesAggregationIntegrationTest {

    private static final BusinessDate TARGET = BusinessDate.parse("20260118");
    private static final BusinessDate OTHER_DAY = BusinessDate.parse("20260117");

    @Autowired
    DailySalesAggregationService aggregationService;

    @Autowired
    OrderRepository orderRepository;

    @Autowired
    DailySalesRepository dailySalesRepository;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @BeforeEach
    void clearTables() {
        jdbcTemplate.update("DELETE FROM daily_sales");
        jdbcTemplate.update("DELETE FROM orders");
    }

    private static Order order(String orderNumber, BusinessDate businessDate,
                               String productCode, int quantity, String unitPrice) {
        return new Order(
                OrderNumber.of(orderNumber),
                businessDate,
                CustomerCode.of("C001"),
                ProductCode.of(productCode),
                Quantity.of(quantity),
                Money.parse(unitPrice),
                LocalDateTime.of(2026, 1, 18, 9, 0));
    }

    @Test
    @DisplayName("商品ごとに件数・数量・金額を積み上げる")
    void 商品ごとに集計する() {
        orderRepository.saveAll(List.of(
                order("A-1", TARGET, "P001", 3, "1000.00"),
                order("A-2", TARGET, "P001", 2, "1000.00"),
                order("A-3", TARGET, "P002", 1, "150.50")));

        AggregationResult result = aggregationService.aggregate(TARGET);

        assertThat(result.registeredRows()).isEqualTo(2);
        assertThat(result.totalOrderCount()).isEqualTo(3);
        assertThat(result.totalAmount()).isEqualTo(Money.parse("5150.50"));

        List<DailySales> sales = dailySalesRepository.findByBusinessDate(TARGET);
        assertThat(sales).hasSize(2);
        assertThat(sales.get(0).productCode()).isEqualTo(ProductCode.of("P001"));
        assertThat(sales.get(0).orderCount()).isEqualTo(2);
        assertThat(sales.get(0).totalQuantity()).isEqualTo(5);
        assertThat(sales.get(0).totalAmount()).isEqualTo(Money.parse("5000.00"));
    }

    @Test
    @DisplayName("他の業務日付の注文は混ざらない")
    void 対象日以外は集計しない() {
        orderRepository.saveAll(List.of(
                order("B-1", TARGET, "P001", 1, "100.00"),
                order("B-2", OTHER_DAY, "P001", 99, "100.00")));

        aggregationService.aggregate(TARGET);

        List<DailySales> sales = dailySalesRepository.findByBusinessDate(TARGET);
        assertThat(sales).hasSize(1);
        assertThat(sales.get(0).totalQuantity()).isEqualTo(1);
    }

    @Test
    @DisplayName("二度集計しても結果は変わらない（再開後に走っても二重計上しない）")
    void 何度実行しても同じ結果になる() {
        orderRepository.saveAll(List.of(
                order("C-1", TARGET, "P001", 2, "500.00"),
                order("C-2", TARGET, "P002", 1, "300.00")));

        AggregationResult first = aggregationService.aggregate(TARGET);
        AggregationResult second = aggregationService.aggregate(TARGET);

        assertThat(second.deletedRows())
                .as("2 回目は既存行を削除してから入れ直す")
                .isEqualTo(first.registeredRows());
        assertThat(second.registeredRows()).isEqualTo(first.registeredRows());
        assertThat(second.totalAmount()).isEqualTo(first.totalAmount());
        assertThat(dailySalesRepository.findByBusinessDate(TARGET)).hasSize(2);
    }

    @Test
    @DisplayName("再開で注文が増えたあとに集計すると、増えた分が反映される")
    void 再集計で増分が反映される() {
        orderRepository.saveAll(List.of(order("D-1", TARGET, "P001", 1, "100.00")));
        aggregationService.aggregate(TARGET);

        // 再開して残りが取り込まれた状況。
        orderRepository.saveAll(List.of(order("D-2", TARGET, "P001", 4, "100.00")));
        AggregationResult result = aggregationService.aggregate(TARGET);

        assertThat(result.totalAmount()).isEqualTo(Money.parse("500.00"));
        assertThat(dailySalesRepository.findByBusinessDate(TARGET))
                .singleElement()
                .satisfies(sales -> {
                    assertThat(sales.orderCount()).isEqualTo(2);
                    assertThat(sales.totalQuantity()).isEqualTo(5);
                });
    }

    @Test
    @DisplayName("対象の注文が 1 件も無ければ、日次売上は作らない")
    void 対象が無ければ何も登録しない() {
        AggregationResult result = aggregationService.aggregate(TARGET);

        assertThat(result.registeredRows()).isZero();
        assertThat(result.totalAmount()).isEqualTo(Money.ZERO);
        assertThat(dailySalesRepository.findByBusinessDate(TARGET)).isEmpty();
    }
}
