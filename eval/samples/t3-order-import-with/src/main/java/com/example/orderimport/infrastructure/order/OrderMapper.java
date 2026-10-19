package com.example.orderimport.infrastructure.order;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;
import java.util.List;

/**
 * orders 表への Mapper。SQL は OrderMapper.xml に書く。
 */
@Mapper
public interface OrderMapper {

    void upsertAll(@Param("orders") List<OrderRecord> orders);

    OrderSalesSummaryRecord selectSalesSummary(@Param("orderDate") LocalDate orderDate);
}
