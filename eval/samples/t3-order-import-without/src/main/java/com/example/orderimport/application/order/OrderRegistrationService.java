package com.example.orderimport.application.order;

import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.domain.repository.OrderRepository;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * 検証済みの注文を登録するユースケース。
 *
 * <p>トランザクションはチャンク単位でステップ側が張るため、ここでは
 * {@code @Transactional} を付けず呼び出し元の境界に従う。再開時はコミット済みの
 * チャンクまでが確定し、その続きから読み直される。
 */
@Service
public class OrderRegistrationService {

    private final OrderRepository orderRepository;

    public OrderRegistrationService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void register(List<Order> orders) {
        if (orders.isEmpty()) {
            return;
        }
        orderRepository.saveAll(orders);
    }
}
