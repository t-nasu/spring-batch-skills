package com.example.orderimport.presentation.listener;

import com.example.orderimport.application.order.ImportOrderCommand;
import com.example.orderimport.application.order.OrderValidationException;
import com.example.orderimport.domain.model.order.Order;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.SkipListener;

/**
 * 読み飛ばした行を記録する。
 *
 * <p>「飛ばして続ける」は、どの行をなぜ飛ばしたかが後から分かって初めて運用できる。
 * 取引先への差し戻しに使えるよう、行番号と理由を 1 行ずつ WARN で残す。
 */
public class SkippedOrderLineListener implements SkipListener<ImportOrderCommand, Order> {

    private static final Logger log = LoggerFactory.getLogger(SkippedOrderLineListener.class);

    private final AtomicLong skippedCount = new AtomicLong();

    /** 検証に失敗した行（商品・顧客がマスタにない、数量が 1 未満、書式不正など）。 */
    @Override
    public void onSkipInProcess(ImportOrderCommand command, Throwable cause) {
        long count = skippedCount.incrementAndGet();
        if (cause instanceof OrderValidationException validation) {
            log.warn("[読み飛ばし {} 件目] {}", count, validation.getMessage());
        } else {
            log.warn("[読み飛ばし {} 件目] {} 行目の処理に失敗しました", count, command.lineNumber(), cause);
        }
    }

    /** 列数が合わないなど、行として解釈できなかったもの。 */
    @Override
    public void onSkipInRead(Throwable cause) {
        long count = skippedCount.incrementAndGet();
        log.warn("[読み飛ばし {} 件目] 行を解釈できませんでした: {}", count, cause.getMessage());
    }

    /**
     * 登録時のスキップ。
     *
     * <p>書き込み例外はスキップ対象に設定していないため通常は呼ばれない。想定外の
     * 経路で読み飛ばしが起きたことに気づけるよう、記録だけは残す。
     */
    @Override
    public void onSkipInWrite(Order order, Throwable cause) {
        long count = skippedCount.incrementAndGet();
        log.warn("[読み飛ばし {} 件目] 注文 {} の登録に失敗しました",
                count, order.orderNumber(), cause);
    }
}
