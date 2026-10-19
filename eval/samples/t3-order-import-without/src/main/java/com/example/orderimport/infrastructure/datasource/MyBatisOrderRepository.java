package com.example.orderimport.infrastructure.datasource;

import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Money;
import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.model.sales.DailySales;
import com.example.orderimport.domain.repository.OrderRepository;
import com.example.orderimport.infrastructure.datasource.dto.DailySalesRow;
import com.example.orderimport.infrastructure.datasource.dto.OrderRow;
import com.example.orderimport.infrastructure.datasource.mapper.OrderMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * {@link OrderRepository} の MyBatis 実装。
 */
@Repository
public class MyBatisOrderRepository implements OrderRepository {

    /**
     * 1 文の INSERT にまとめる最大行数。
     *
     * <p>チャンクサイズを大きくしても 1 文が max_allowed_packet を超えないよう、
     * ここで上限を設けて分割する。
     */
    private static final int MAX_ROWS_PER_STATEMENT = 1_000;

    private final OrderMapper orderMapper;
    private final Clock clock;

    public MyBatisOrderRepository(OrderMapper orderMapper, Clock clock) {
        this.orderMapper = orderMapper;
        this.clock = clock;
    }

    @Override
    public void saveAll(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }
        LocalDateTime createdAt = LocalDateTime.now(clock);
        List<OrderRow> rows = orders.stream()
                .map(order -> toRow(order, createdAt))
                .toList();

        for (int from = 0; from < rows.size(); from += MAX_ROWS_PER_STATEMENT) {
            int to = Math.min(from + MAX_ROWS_PER_STATEMENT, rows.size());
            orderMapper.insertAll(rows.subList(from, to));
        }
    }

    @Override
    public List<DailySales> aggregateDailySales(BusinessDate businessDate) {
        return orderMapper.aggregateDailySales(businessDate.value()).stream()
                .map(MyBatisOrderRepository::toDailySales)
                .toList();
    }

    private static OrderRow toRow(Order order, LocalDateTime createdAt) {
        return new OrderRow(
                order.orderNumber().value(),
                order.businessDate().value(),
                order.customerCode().value(),
                order.productCode().value(),
                order.quantity().value(),
                order.unitPrice().value(),
                order.amount().value(),
                order.orderedAt(),
                createdAt);
    }

    private static DailySales toDailySales(DailySalesRow row) {
        return new DailySales(
                BusinessDate.of(row.businessDate()),
                ProductCode.of(row.productCode()),
                row.orderCount(),
                row.totalQuantity(),
                Money.of(row.totalAmount()));
    }
}
