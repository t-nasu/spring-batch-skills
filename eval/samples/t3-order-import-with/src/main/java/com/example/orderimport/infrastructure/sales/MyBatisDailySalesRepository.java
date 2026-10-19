package com.example.orderimport.infrastructure.sales;

import com.example.orderimport.domain.sales.DailySales;
import com.example.orderimport.domain.sales.DailySalesRepository;
import org.springframework.stereotype.Repository;

@Repository
public class MyBatisDailySalesRepository implements DailySalesRepository {

    private final DailySalesMapper dailySalesMapper;

    public MyBatisDailySalesRepository(DailySalesMapper dailySalesMapper) {
        this.dailySalesMapper = dailySalesMapper;
    }

    @Override
    public void save(DailySales dailySales) {
        dailySalesMapper.upsert(toRecord(dailySales));
    }

    private static DailySalesRecord toRecord(DailySales dailySales) {
        DailySalesRecord record = new DailySalesRecord();
        record.setBusinessDate(dailySales.businessDate().value());
        record.setOrderCount(dailySales.orderCount());
        record.setTotalAmount(dailySales.totalAmount().value());
        return record;
    }
}
