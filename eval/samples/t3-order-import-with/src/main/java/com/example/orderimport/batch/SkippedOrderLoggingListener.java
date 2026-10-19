package com.example.orderimport.batch;

import com.example.orderimport.domain.order.Order;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;
import org.springframework.stereotype.Component;

/**
 * 読み飛ばした行をログに残す。あとで取引先に問い合わせられるようにするため。
 */
@Component
public class SkippedOrderLoggingListener implements SkipListener<Order, Order> {

    private static final Logger log = LoggerFactory.getLogger(SkippedOrderLoggingListener.class);

    @Override
    public void onSkipInRead(Throwable cause) {
        log.warn("CSV の行を読み飛ばした: {}", cause.getMessage());
    }

    @Override
    public void onSkipInProcess(Order order, Throwable cause) {
        log.warn("注文を読み飛ばした: orderNumber={}, 理由={}", order.orderNumber(), cause.getMessage());
    }

    @Override
    public void onSkipInWrite(Order order, Throwable cause) {
        log.warn("注文の登録を読み飛ばした: orderNumber={}, 理由={}", order.orderNumber(), cause.getMessage());
    }
}
