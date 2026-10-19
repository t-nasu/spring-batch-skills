package com.example.salesbatch.presentation.batch.reader;

import com.example.salesbatch.application.service.MonthlySalesGroupLoader;
import com.example.salesbatch.application.service.MonthlySalesGroupLoaderFactory;
import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.SalesGroupKey;
import com.example.salesbatch.domain.model.TargetMonth;

import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemStreamReader;

/**
 * 集計単位（店舗 × 商品）ごとの集計結果を 1 件ずつ読み出す {@code ItemReader}。
 *
 * <p>実際の読み込みと集計は {@link MonthlySalesGroupLoader}（アプリケーション層）が行い、
 * このクラスは Spring Batch との接続——進捗を {@link ExecutionContext} に保存し、
 * 再開時に復元すること——だけを担う。</p>
 *
 * <p>進捗として保存するのは「登録まで完了した最後の集計単位」と「読み飛ばし件数」。
 * これらはチャンクのコミットと同じトランザクションで保存されるため、
 * 途中で落ちても、コミット済みの位置から正確に再開できる。</p>
 */
public class MonthlySalesSummaryItemReader implements ItemStreamReader<MonthlySalesSummary> {

    private static final Logger log = LoggerFactory.getLogger(MonthlySalesSummaryItemReader.class);

    static final String CONTEXT_KEY_LAST_STORE_ID = "monthlySales.lastCompletedStoreId";
    static final String CONTEXT_KEY_LAST_PRODUCT_ID = "monthlySales.lastCompletedProductId";
    static final String CONTEXT_KEY_SKIPPED_COUNT = "monthlySales.skippedCount";

    private final MonthlySalesGroupLoaderFactory loaderFactory;
    private final TargetMonth targetMonth;

    private MonthlySalesGroupLoader loader;

    public MonthlySalesSummaryItemReader(
            MonthlySalesGroupLoaderFactory loaderFactory, TargetMonth targetMonth) {
        this.loaderFactory = Objects.requireNonNull(loaderFactory, "loaderFactory は必須です");
        this.targetMonth = Objects.requireNonNull(targetMonth, "targetMonth は必須です");
    }

    @Override
    public void open(ExecutionContext executionContext) {
        loader = loaderFactory.create(targetMonth);

        if (executionContext.containsKey(CONTEXT_KEY_LAST_STORE_ID)) {
            SalesGroupKey lastCompleted = SalesGroupKey.of(
                    executionContext.getString(CONTEXT_KEY_LAST_STORE_ID),
                    executionContext.getString(CONTEXT_KEY_LAST_PRODUCT_ID));
            loader.resumeAfter(lastCompleted, executionContext.getInt(CONTEXT_KEY_SKIPPED_COUNT, 0));
        }
    }

    @Override
    public MonthlySalesSummary read() {
        return loader.next().orElse(null);
    }

    @Override
    public void update(ExecutionContext executionContext) {
        loader.lastCompletedGroupKey().ifPresent(groupKey -> {
            executionContext.putString(CONTEXT_KEY_LAST_STORE_ID, groupKey.storeId().value());
            executionContext.putString(CONTEXT_KEY_LAST_PRODUCT_ID, groupKey.productId().value());
        });
        executionContext.putInt(CONTEXT_KEY_SKIPPED_COUNT, loader.committedSkippedCount());
    }

    @Override
    public void close() {
        if (loader != null) {
            log.info("取引明細の読み込みを終了しました: targetMonth={} 集計単位={}件 読み飛ばし={}件",
                    targetMonth.asString(), loader.completedGroupCount(), loader.skippedCount());
        }
        loader = null;
    }
}
