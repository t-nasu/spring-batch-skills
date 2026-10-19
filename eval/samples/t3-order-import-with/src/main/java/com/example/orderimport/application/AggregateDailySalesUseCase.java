package com.example.orderimport.application;

import com.example.orderimport.domain.order.OrderRepository;
import com.example.orderimport.domain.order.OrderSalesSummary;
import com.example.orderimport.domain.sales.DailySales;
import com.example.orderimport.domain.sales.DailySalesRepository;
import com.example.orderimport.domain.shared.BusinessDate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 業務日付の日次売上を集計して登録する（仕様 4-3）。
 *
 * <p>集計の対象は注文表に入っている「その業務日付の注文」すべてとする。
 * 今回の取り込み分だけを数えないのは、再開や再実行をしても結果が同じになるようにするためである。
 */
@Component
public class AggregateDailySalesUseCase {

    private static final Logger log = LoggerFactory.getLogger(AggregateDailySalesUseCase.class);

    private final OrderRepository orderRepository;
    private final DailySalesRepository dailySalesRepository;

    public AggregateDailySalesUseCase(OrderRepository orderRepository, DailySalesRepository dailySalesRepository) {
        this.orderRepository = orderRepository;
        this.dailySalesRepository = dailySalesRepository;
    }

    @Transactional
    public DailySales execute(BusinessDate targetDate) {
        OrderSalesSummary summary = orderRepository.summarizeByOrderDate(targetDate);
        DailySales dailySales = DailySales.of(targetDate, summary.orderCount(), summary.totalAmount());
        dailySalesRepository.save(dailySales);
        log.info("日次売上を登録した: businessDate={}, orderCount={}, totalAmount={}",
                dailySales.businessDate(), dailySales.orderCount(), dailySales.totalAmount());
        return dailySales;
    }
}
