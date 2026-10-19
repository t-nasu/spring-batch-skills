package com.example.orderimport.application;

import com.example.orderimport.domain.customer.CustomerRepository;
import com.example.orderimport.domain.order.InvalidOrderException;
import com.example.orderimport.domain.order.Order;
import com.example.orderimport.domain.order.OrderValidator;
import com.example.orderimport.domain.product.ProductRepository;
import org.springframework.stereotype.Component;

/**
 * 注文 1 件を取り込んでよいかを確かめる（仕様 4-1）。
 *
 * <p>登録そのものは chunk の Writer がまとめて行うため、ここでは検証だけを受け持つ。
 * トランザクションは chunk 単位で Spring Batch が張るので、ここでは張らない。
 */
@Component
public class ImportOrderUseCase {

    private final OrderValidator orderValidator;

    public ImportOrderUseCase(CustomerRepository customerRepository, ProductRepository productRepository) {
        this.orderValidator = new OrderValidator(customerRepository, productRepository);
    }

    /**
     * @return 取り込んでよい注文
     * @throws InvalidOrderException 取り込めない注文のとき
     */
    public Order validate(Order order) {
        orderValidator.validate(order);
        return order;
    }
}
