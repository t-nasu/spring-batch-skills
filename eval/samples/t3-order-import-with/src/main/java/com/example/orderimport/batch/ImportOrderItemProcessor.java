package com.example.orderimport.batch;

import com.example.orderimport.application.ImportOrderUseCase;
import com.example.orderimport.domain.order.Order;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.stereotype.Component;

/**
 * 読み込んだ注文をユースケースに渡すだけの入れ物。業務判断はここには書かない。
 */
@Component
public class ImportOrderItemProcessor implements ItemProcessor<Order, Order> {

    private final ImportOrderUseCase importOrderUseCase;

    public ImportOrderItemProcessor(ImportOrderUseCase importOrderUseCase) {
        this.importOrderUseCase = importOrderUseCase;
    }

    @Override
    public Order process(Order order) {
        return importOrderUseCase.validate(order);
    }
}
