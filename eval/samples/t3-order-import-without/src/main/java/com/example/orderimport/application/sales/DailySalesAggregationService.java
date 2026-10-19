package com.example.orderimport.application.sales;

import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Money;
import com.example.orderimport.domain.model.sales.DailySales;
import com.example.orderimport.domain.repository.DailySalesRepository;
import com.example.orderimport.domain.repository.OrderRepository;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 業務日付の日次売上を集計して登録するユースケース。
 *
 * <p>対象業務日付の既存行を削除してから入れ直すため、何度実行しても結果は同じに
 * なる。取り込みステップが途中から再開されたあとに続けて実行されても、売上が
 * 二重計上されることはない。
 *
 * <p>注文は最大 30 万件になりうるので、集計は SQL の GROUP BY に任せ、アプリ側へ
 * 取り出すのは商品単位に畳んだあとの行だけにしている。
 */
@Service
public class DailySalesAggregationService {

    private static final Logger log = LoggerFactory.getLogger(DailySalesAggregationService.class);

    private final OrderRepository orderRepository;
    private final DailySalesRepository dailySalesRepository;

    public DailySalesAggregationService(OrderRepository orderRepository,
                                        DailySalesRepository dailySalesRepository) {
        this.orderRepository = orderRepository;
        this.dailySalesRepository = dailySalesRepository;
    }

    @Transactional
    public AggregationResult aggregate(BusinessDate businessDate) {
        int deleted = dailySalesRepository.deleteByBusinessDate(businessDate);
        if (deleted > 0) {
            log.info("業務日付 {} の既存の日次売上 {} 件を削除しました（再集計）", businessDate, deleted);
        }

        List<DailySales> aggregated = orderRepository.aggregateDailySales(businessDate);
        dailySalesRepository.saveAll(aggregated);

        Money totalAmount = aggregated.stream()
                .map(DailySales::totalAmount)
                .reduce(Money.ZERO, Money::plus);
        long totalOrderCount = aggregated.stream()
                .mapToLong(DailySales::orderCount)
                .sum();

        AggregationResult result =
                new AggregationResult(businessDate, deleted, aggregated.size(), totalOrderCount, totalAmount);
        log.info("日次売上を集計しました: {}", result);
        return result;
    }

    /**
     * 集計結果の要約。
     *
     * @param businessDate    業務日付
     * @param deletedRows     再集計のために削除した既存行数
     * @param registeredRows  登録した日次売上の行数（＝対象商品数）
     * @param totalOrderCount 集計対象になった注文件数
     * @param totalAmount     売上金額合計
     */
    public record AggregationResult(
            BusinessDate businessDate,
            int deletedRows,
            int registeredRows,
            long totalOrderCount,
            Money totalAmount) {

        @Override
        public String toString() {
            return "業務日付=%s, 削除=%d 件, 登録=%d 件, 対象注文=%d 件, 売上合計=%s"
                    .formatted(businessDate, deletedRows, registeredRows, totalOrderCount, totalAmount);
        }
    }
}
