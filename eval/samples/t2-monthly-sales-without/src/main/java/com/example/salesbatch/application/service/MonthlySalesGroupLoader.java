package com.example.salesbatch.application.service;

import com.example.salesbatch.domain.exception.InvalidSalesTransactionException;
import com.example.salesbatch.domain.model.MonthlySalesSummary;
import com.example.salesbatch.domain.model.RawSalesTransaction;
import com.example.salesbatch.domain.model.SalesGroupKey;
import com.example.salesbatch.domain.model.SalesTransaction;
import com.example.salesbatch.domain.model.TargetMonth;
import com.example.salesbatch.domain.repository.SalesTransactionCursor;
import com.example.salesbatch.domain.repository.SalesTransactionRepository;
import com.example.salesbatch.domain.service.MonthlySalesAggregation;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 対象年月の取引明細を店舗 × 商品の順に読み進め、集計単位ごとに
 * {@link MonthlySalesSummary} を 1 件ずつ切り出すアプリケーションサービス。
 *
 * <p>処理の流れ:</p>
 * <ol>
 *   <li>{@code (店舗 ID, 商品 ID, 取引 ID)} 昇順でページ単位（{@code pageSize} 件）に読み込む</li>
 *   <li>不正データは {@link InvalidTransactionSkipPolicy} に記録して読み飛ばす</li>
 *   <li>集計単位が切り替わった時点で、直前の集計単位の結果を 1 件返す</li>
 * </ol>
 *
 * <p>明細を全件メモリに載せず、保持するのは「読み込み中の 1 ページ」と
 * 「集計中の 1 グループの合計値」だけなので、800 万件規模でもメモリ使用量は一定。</p>
 *
 * <p>中断からの再開は {@link #resumeAfter(SalesGroupKey, int)} で行う。
 * {@link #lastCompletedGroupKey()} が指すグループまでは結果を返し切っているので、
 * その次のグループから読み直せばよい。グループ単位で区切って返しているため、
 * 再開位置が集計の途中にかかることはない。</p>
 *
 * <p>ステートフルであり、スレッドセーフではない（ステップスコープで 1 インスタンスを使う）。</p>
 */
public class MonthlySalesGroupLoader {

    private static final Logger log = LoggerFactory.getLogger(MonthlySalesGroupLoader.class);

    private final SalesTransactionRepository salesTransactionRepository;
    private final InvalidTransactionSkipPolicy skipPolicy;
    private final TargetMonth targetMonth;
    private final int pageSize;

    private final Deque<RawSalesTransaction> buffer = new ArrayDeque<>();

    /**
     * 読み飛ばした明細のうち、まだ「確定済み」と数えていないものの位置（店舗 ID, 商品 ID）。
     *
     * <p>明細はキー順に読むため、読み飛ばした明細の位置も昇順に並ぶ。集計単位を確定するたびに、
     * その集計単位以前の位置のものを取り出して {@link #committedSkippedCount} に数える。
     * こうすると、再開時に読み直す範囲の読み飛ばしを二重に数えずに済む。</p>
     */
    private final Deque<SkippedRowPosition> pendingSkips = new ArrayDeque<>();

    private SalesTransactionCursor cursor = SalesTransactionCursor.start();
    private boolean started;
    private boolean sourceExhausted;

    private MonthlySalesAggregation current;
    private SalesGroupKey lastCompletedGroupKey;
    private int committedSkippedCount;
    private long completedGroupCount;

    /** 読み飛ばした明細の位置。 */
    private record SkippedRowPosition(String storeId, String productId) {

        static SkippedRowPosition of(RawSalesTransaction raw) {
            return new SkippedRowPosition(
                    Objects.requireNonNullElse(raw.storeId(), ""),
                    Objects.requireNonNullElse(raw.productId(), ""));
        }

        /** 指定した集計単位以前の位置か。 */
        boolean isAtOrBefore(SalesGroupKey groupKey) {
            int storeComparison = storeId.compareTo(groupKey.storeId().value());
            if (storeComparison != 0) {
                return storeComparison < 0;
            }
            return productId.compareTo(groupKey.productId().value()) <= 0;
        }
    }

    public MonthlySalesGroupLoader(
            SalesTransactionRepository salesTransactionRepository,
            InvalidTransactionSkipPolicy skipPolicy,
            TargetMonth targetMonth,
            int pageSize) {
        this.salesTransactionRepository =
                Objects.requireNonNull(salesTransactionRepository, "salesTransactionRepository は必須です");
        this.skipPolicy = Objects.requireNonNull(skipPolicy, "skipPolicy は必須です");
        this.targetMonth = Objects.requireNonNull(targetMonth, "targetMonth は必須です");
        if (pageSize < 1) {
            throw new IllegalArgumentException("pageSize は 1 以上です: " + pageSize);
        }
        this.pageSize = pageSize;
    }

    /**
     * 中断したジョブを再開するために、読み込み位置と読み飛ばし件数を復元する。
     * 読み込みを始める前に呼ぶこと。
     *
     * @param lastCompletedGroupKey 前回のジョブ実行で登録まで完了している最後の集計単位
     * @param alreadySkippedCount   前回までに読み飛ばした不正データの件数
     */
    public void resumeAfter(SalesGroupKey lastCompletedGroupKey, int alreadySkippedCount) {
        Objects.requireNonNull(lastCompletedGroupKey, "lastCompletedGroupKey は必須です");
        if (started) {
            throw new IllegalStateException("読み込み開始後に再開位置は変更できません");
        }
        this.lastCompletedGroupKey = lastCompletedGroupKey;
        this.cursor = SalesTransactionCursor.afterGroup(lastCompletedGroupKey);
        this.committedSkippedCount = alreadySkippedCount;
        this.skipPolicy.restore(alreadySkippedCount);
        log.info("前回の続きから集計を再開します: targetMonth={} 再開位置={} 読み飛ばし済み={}件",
                targetMonth.asString(), lastCompletedGroupKey, alreadySkippedCount);
    }

    /**
     * 次の集計単位の集計結果を返す。
     *
     * @return 集計結果。読み込む明細がもう無ければ {@link Optional#empty()}
     */
    public Optional<MonthlySalesSummary> next() {
        started = true;
        while (true) {
            RawSalesTransaction raw = nextTransaction();
            if (raw == null) {
                // これ以上読む明細が無いので、残っている読み飛ばしはすべて確定扱いにする
                committedSkippedCount += pendingSkips.size();
                pendingSkips.clear();
                return Optional.ofNullable(completeCurrentGroup());
            }

            SalesTransaction transaction;
            try {
                transaction = raw.toSalesTransaction();
            } catch (InvalidSalesTransactionException e) {
                pendingSkips.add(SkippedRowPosition.of(raw));
                skipPolicy.record(e);
                continue;
            }

            SalesGroupKey groupKey = transaction.groupKey();
            if (current == null) {
                current = new MonthlySalesAggregation(targetMonth, groupKey);
                current.add(transaction);
                continue;
            }
            if (current.groupKey().equals(groupKey)) {
                current.add(transaction);
                continue;
            }

            // 集計単位が切り替わった。直前のグループを確定して返し、現在の明細から次のグループを始める。
            MonthlySalesSummary completed = completeCurrentGroup();
            current = new MonthlySalesAggregation(targetMonth, groupKey);
            current.add(transaction);
            return Optional.of(completed);
        }
    }

    /** 登録まで完了している最後の集計単位。まだ 1 件も返していなければ {@link Optional#empty()}。 */
    public Optional<SalesGroupKey> lastCompletedGroupKey() {
        return Optional.ofNullable(lastCompletedGroupKey);
    }

    /**
     * {@link #lastCompletedGroupKey()} までの範囲で読み飛ばした不正データの件数。
     *
     * <p>再開時に読み直す範囲（確定した集計単位より後ろ）の読み飛ばしは含めないので、
     * 進捗として保存しても件数が二重に数えられることはない。</p>
     */
    public int committedSkippedCount() {
        return committedSkippedCount;
    }

    /** このインスタンスがこれまでに読み飛ばした不正データの件数（確定前のものも含む）。 */
    public int skippedCount() {
        return skipPolicy.skippedCount();
    }

    /** このインスタンスが返した集計単位の件数。 */
    public long completedGroupCount() {
        return completedGroupCount;
    }

    /**
     * 集計中のグループを確定する。
     *
     * @return 確定した集計結果。集計中のグループが無ければ {@code null}
     */
    private MonthlySalesSummary completeCurrentGroup() {
        if (current == null) {
            return null;
        }
        MonthlySalesSummary summary = current.toSummary();
        lastCompletedGroupKey = current.groupKey();
        while (!pendingSkips.isEmpty() && pendingSkips.peek().isAtOrBefore(lastCompletedGroupKey)) {
            pendingSkips.poll();
            committedSkippedCount++;
        }
        completedGroupCount++;
        current = null;
        return summary;
    }

    /**
     * 次の取引明細を 1 件取り出す。バッファが空なら次のページを読み込む。
     *
     * @return 取引明細。対象年月の明細を読み切っていれば {@code null}
     */
    private RawSalesTransaction nextTransaction() {
        if (buffer.isEmpty()) {
            fetchNextPage();
        }
        return buffer.poll();
    }

    private void fetchNextPage() {
        if (sourceExhausted) {
            return;
        }
        List<RawSalesTransaction> page = salesTransactionRepository.findPage(targetMonth, cursor, pageSize);
        if (page.size() < pageSize) {
            sourceExhausted = true;
        }
        if (page.isEmpty()) {
            return;
        }
        RawSalesTransaction last = page.get(page.size() - 1);
        cursor = SalesTransactionCursor.afterRow(last.storeId(), last.productId(), last.transactionId());
        buffer.addAll(page);
    }
}
