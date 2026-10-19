package com.example.orderimport.infrastructure.order;

import com.example.orderimport.domain.order.Order;
import com.example.orderimport.domain.order.OrderRepository;
import com.example.orderimport.domain.order.OrderSalesSummary;
import com.example.orderimport.domain.shared.BusinessDate;
import com.example.orderimport.domain.shared.Money;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class MyBatisOrderRepository implements OrderRepository {

    private final OrderMapper orderMapper;

    public MyBatisOrderRepository(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @Override
    public void saveAll(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }
        orderMapper.upsertAll(orders.stream().map(MyBatisOrderRepository::toRecord).toList());
    }

    @Override
    public OrderSalesSummary summarizeByOrderDate(BusinessDate orderDate) {
        OrderSalesSummaryRecord record = orderMapper.selectSalesSummary(orderDate.value());
        if (record == null || record.getTotalAmount() == null) {
            return OrderSalesSummary.EMPTY;
        }
        return new OrderSalesSummary(record.getOrderCount(), Money.of(record.getTotalAmount()));
    }

    private static OrderRecord toRecord(Order order) {
        OrderRecord record = new OrderRecord();
        record.setOrderNumber(order.orderNumber().value());
        record.setCustomerCode(order.customerCode().value());
        record.setProductCode(order.productCode().value());
        record.setQuantity(order.quantity().value());
        record.setUnitPrice(order.unitPrice().value());
        record.setAmount(order.amount().value());
        record.setOrderedAt(order.orderedAt());
        record.setOrderDate(order.orderDate().value());
        return record;
    }
}
