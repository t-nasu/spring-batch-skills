package com.example.orderimport.domain.order;

import com.example.orderimport.domain.customer.CustomerRepository;
import com.example.orderimport.domain.product.ProductRepository;

/**
 * 注文を取り込んでよいかを判断するドメインサービス。
 *
 * <p>数量が 1 以上であることは {@link Quantity} が受け持つため、ここでは
 * 他の集約を参照する必要がある「顧客コード・商品コードが存在すること」だけを確かめる（仕様 4-1）。
 */
public class OrderValidator {

    private final CustomerRepository customerRepository;
    private final ProductRepository productRepository;

    public OrderValidator(CustomerRepository customerRepository, ProductRepository productRepository) {
        this.customerRepository = customerRepository;
        this.productRepository = productRepository;
    }

    /**
     * @throws InvalidOrderException 取り込めない注文のとき
     */
    public void validate(Order order) {
        if (!customerRepository.exists(order.customerCode())) {
            throw new InvalidOrderException(
                    "顧客コードが存在しない: orderNumber=" + order.orderNumber() + ", customerCode=" + order.customerCode());
        }
        if (!productRepository.exists(order.productCode())) {
            throw new InvalidOrderException(
                    "商品コードが存在しない: orderNumber=" + order.orderNumber() + ", productCode=" + order.productCode());
        }
    }
}
