package com.example.orderimport.presentation.step;

import com.example.orderimport.application.order.ImportOrderCommand;
import com.example.orderimport.application.order.OrderLineImportService;
import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Order;
import org.springframework.batch.infrastructure.item.ItemProcessor;

/**
 * 読み取った 1 行を検証済みの注文に変換する。
 *
 * <p>処理そのものはユースケースに委ね、ここは Spring Batch との接続だけを担う。
 * 検証に失敗すると {@code OrderValidationException} が送出され、ステップの
 * スキップ設定によってその行は読み飛ばされる。
 */
public class OrderItemProcessor implements ItemProcessor<ImportOrderCommand, Order> {

    private final OrderLineImportService orderLineImportService;
    private final BusinessDate businessDate;

    public OrderItemProcessor(OrderLineImportService orderLineImportService, BusinessDate businessDate) {
        this.orderLineImportService = orderLineImportService;
        this.businessDate = businessDate;
    }

    @Override
    public Order process(ImportOrderCommand command) {
        return orderLineImportService.toOrder(command, businessDate);
    }
}
