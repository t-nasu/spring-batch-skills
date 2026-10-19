package com.example.orderimport.infrastructure.order;

import com.example.orderimport.domain.order.Order;
import com.example.orderimport.domain.order.OrderRepository;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 検証を通った注文を chunk 単位で注文表に登録する（仕様 4-2）。
 * 更新は必ずリポジトリを経由する。
 */
@Component
public class OrderItemWriter implements ItemWriter<Order> {

    private final OrderRepository orderRepository;

    public OrderItemWriter(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    @Override
    public void write(Chunk<? extends Order> chunk) {
        orderRepository.saveAll(List.copyOf(chunk.getItems()));
    }
}
