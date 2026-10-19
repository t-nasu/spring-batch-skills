package com.example.orderimport.application;

import com.example.orderimport.domain.order.Order;
import com.example.orderimport.domain.order.OrderRepository;
import com.example.orderimport.domain.order.OrderSalesSummary;
import com.example.orderimport.domain.sales.DailySales;
import com.example.orderimport.domain.sales.DailySalesRepository;
import com.example.orderimport.domain.shared.BusinessDate;
import com.example.orderimport.domain.shared.Money;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AggregateDailySalesUseCaseTest {

    private final BusinessDate targetDate = BusinessDate.of(LocalDate.of(2026, 10, 18));
    private final List<DailySales> saved = new ArrayList<>();
    private final DailySalesRepository dailySalesRepository = saved::add;

    @Test
    @DisplayName("業務日付の集計結果を日次売上として登録する")
    void savesSummaryOfTargetDateAsDailySales() {
        AggregateDailySalesUseCase useCase = new AggregateDailySalesUseCase(
                summarizing(new OrderSalesSummary(3L, Money.of(new BigDecimal("4500.00")))),
                dailySalesRepository);

        DailySales result = useCase.execute(targetDate);

        assertThat(result).isEqualTo(
                DailySales.of(targetDate, 3L, Money.of(new BigDecimal("4500.00"))));
        assertThat(saved).containsExactly(result);
    }

    @Test
    @DisplayName("注文が一件もなければ件数ゼロで登録する")
    void savesZeroWhenNoOrders() {
        AggregateDailySalesUseCase useCase = new AggregateDailySalesUseCase(
                summarizing(OrderSalesSummary.EMPTY), dailySalesRepository);

        DailySales result = useCase.execute(targetDate);

        assertThat(result.orderCount()).isZero();
        assertThat(result.totalAmount()).isEqualTo(Money.ZERO);
        assertThat(saved).hasSize(1);
    }

    private OrderRepository summarizing(OrderSalesSummary summary) {
        return new OrderRepository() {
            @Override
            public void saveAll(List<Order> orders) {
                throw new UnsupportedOperationException("集計では使わない");
            }

            @Override
            public OrderSalesSummary summarizeByOrderDate(BusinessDate orderDate) {
                assertThat(orderDate).isEqualTo(targetDate);
                return summary;
            }
        };
    }
}
