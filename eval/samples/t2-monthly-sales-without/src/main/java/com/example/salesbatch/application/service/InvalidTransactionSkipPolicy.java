package com.example.salesbatch.application.service;

import com.example.salesbatch.application.exception.SkipLimitExceededException;
import com.example.salesbatch.domain.exception.InvalidSalesTransactionException;

import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 不正データの読み飛ばしを管理する。
 *
 * <ul>
 *   <li>読み飛ばした取引 ID を WARN ログに出力する</li>
 *   <li>読み飛ばし件数を数え、上限を超えたら {@link SkipLimitExceededException} を送出する</li>
 *   <li>確定済み（コミット済み）の件数を保持し、中断・再開をまたいで件数を引き継ぐ</li>
 * </ul>
 *
 * <p>ステップ 1 回分の読み込みに対応するステートフルなオブジェクトであり、スレッドセーフではない
 * （ステップスコープで 1 インスタンスを使う）。</p>
 */
public class InvalidTransactionSkipPolicy {

    private static final Logger log = LoggerFactory.getLogger(InvalidTransactionSkipPolicy.class);

    private final int limit;

    /**
     * 読み飛ばし済みの取引 ID。チャンクのリトライなどで同じ明細を二度数えないために保持する。
     * 上限を超えた時点でジョブが落ちるため、保持件数は上限 + 1 件に収まる。
     */
    private final Set<Long> skippedTransactionIds = new HashSet<>();

    private int skippedCount;

    public InvalidTransactionSkipPolicy(int limit) {
        if (limit < 0) {
            throw new IllegalArgumentException("limit は 0 以上です: " + limit);
        }
        this.limit = limit;
    }

    /**
     * 中断したジョブを再開するときに、前回までの読み飛ばし件数を復元する。
     */
    public void restore(int alreadySkippedCount) {
        if (alreadySkippedCount < 0) {
            throw new IllegalArgumentException("alreadySkippedCount は 0 以上です: " + alreadySkippedCount);
        }
        this.skippedCount = alreadySkippedCount;
    }

    /**
     * 不正データを 1 件読み飛ばしたことを記録する。
     *
     * @throws SkipLimitExceededException 読み飛ばし件数が上限を超えた場合
     */
    public void record(InvalidSalesTransactionException exception) {
        if (!skippedTransactionIds.add(exception.transactionId())) {
            // チャンクのリトライ等で同じ明細を再処理した場合。二重計上しない。
            return;
        }
        skippedCount++;
        log.warn("不正データのため読み飛ばしました: transactionId={} reason={} skipped={}/{}",
                exception.transactionId(), exception.reason(), skippedCount, limit);

        if (skippedCount > limit) {
            throw new SkipLimitExceededException(limit, skippedCount, exception);
        }
    }

    /** これまでに読み飛ばした件数。 */
    public int skippedCount() {
        return skippedCount;
    }

    /** 読み飛ばしの上限件数。 */
    public int limit() {
        return limit;
    }
}
