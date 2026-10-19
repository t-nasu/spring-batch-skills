package com.example.orderimport.infrastructure.datasource;

import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Money;
import com.example.orderimport.domain.model.order.ProductCode;
import com.example.orderimport.domain.model.sales.DailySales;
import com.example.orderimport.domain.repository.DailySalesRepository;
import com.example.orderimport.infrastructure.datasource.dto.DailySalesRow;
import com.example.orderimport.infrastructure.datasource.mapper.DailySalesMapper;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * {@link DailySalesRepository} の MyBatis 実装。
 */
@Repository
public class MyBatisDailySalesRepository implements DailySalesRepository {

    /** 1 文の INSERT にまとめる最大行数。 */
    private static final int MAX_ROWS_PER_STATEMENT = 1_000;

    private final DailySalesMapper dailySalesMapper;
    private final Clock clock;

    public MyBatisDailySalesRepository(DailySalesMapper dailySalesMapper, Clock clock) {
        this.dailySalesMapper = dailySalesMapper;
        this.clock = clock;
    }

    @Override
    public int deleteByBusinessDate(BusinessDate businessDate) {
        return dailySalesMapper.deleteByBusinessDate(businessDate.value());
    }

    @Override
    public void saveAll(List<DailySales> dailySales) {
        if (dailySales.isEmpty()) {
            return;
        }
        LocalDateTime aggregatedAt = LocalDateTime.now(clock);
        List<DailySalesRow> rows = dailySales.stream()
                .map(sales -> toRow(sales, aggregatedAt))
                .toList();

        for (int from = 0; from < rows.size(); from += MAX_ROWS_PER_STATEMENT) {
            int to = Math.min(from + MAX_ROWS_PER_STATEMENT, rows.size());
            dailySalesMapper.insertAll(rows.subList(from, to));
        }
    }

    @Override
    public List<DailySales> findByBusinessDate(BusinessDate businessDate) {
        return dailySalesMapper.findByBusinessDate(businessDate.value()).stream()
                .map(MyBatisDailySalesRepository::toDailySales)
                .toList();
    }

    private static DailySalesRow toRow(DailySales sales, LocalDateTime aggregatedAt) {
        return new DailySalesRow(
                sales.businessDate().value(),
                sales.productCode().value(),
                sales.orderCount(),
                sales.totalQuantity(),
                sales.totalAmount().value(),
                aggregatedAt);
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
