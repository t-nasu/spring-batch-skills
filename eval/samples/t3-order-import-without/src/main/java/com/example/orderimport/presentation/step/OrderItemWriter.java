package com.example.orderimport.presentation.step;

import com.example.orderimport.application.order.OrderRegistrationService;
import com.example.orderimport.domain.model.order.Order;
import java.util.List;
import org.springframework.batch.infrastructure.item.Chunk;
import org.springframework.batch.infrastructure.item.ItemWriter;

/**
 * 1 チャンク分の注文を登録する。
 *
 * <p>チャンク全体が 1 トランザクションになる。ここでの登録がコミットされた時点が
 * 再開地点になり、途中で失敗してもコミット済みのチャンクまでは残る。
 */
public class OrderItemWriter implements ItemWriter<Order> {

    private final OrderRegistrationService orderRegistrationService;

    public OrderItemWriter(OrderRegistrationService orderRegistrationService) {
        this.orderRegistrationService = orderRegistrationService;
    }

    @Override
    public void write(Chunk<? extends Order> chunk) {
        orderRegistrationService.register(List.copyOf(chunk.getItems()));
    }
}
