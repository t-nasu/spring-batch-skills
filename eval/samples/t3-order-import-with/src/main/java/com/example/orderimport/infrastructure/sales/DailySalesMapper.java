package com.example.orderimport.infrastructure.sales;

import org.apache.ibatis.annotations.Mapper;

/**
 * daily_sales 表への Mapper。SQL は DailySalesMapper.xml に書く。
 */
@Mapper
public interface DailySalesMapper {

    void upsert(DailySalesRecord dailySales);
}
