package com.example.salesbatch.domain.repository;

import com.example.salesbatch.domain.model.SalesGroupKey;

import java.util.Objects;

/**
 * 取引明細の読み込み位置（キーセットページングのカーソル）。
 *
 * <p>取引明細は {@code (店舗 ID, 商品 ID, 取引 ID)} の昇順で読み込む。
 * OFFSET ではなく「直前に読んだ位置より後ろ」を条件にすることで、
 * 数百万件規模でも後続ページの取得コストが一定になり、中断からの再開も位置指定だけで済む。</p>
 */
public sealed interface SalesTransactionCursor {

    /** 先頭から読み込む。 */
    static SalesTransactionCursor start() {
        return Start.INSTANCE;
    }

    /**
     * 指定した集計単位より後ろから読み込む。中断したジョブを再開するときに使う
     * （登録済みのグループを丸ごと読み飛ばす）。
     */
    static SalesTransactionCursor afterGroup(SalesGroupKey groupKey) {
        return new AfterGroup(groupKey);
    }

    /** 指定した行より後ろから読み込む。ページを継続して取得するときに使う。 */
    static SalesTransactionCursor afterRow(String storeId, String productId, long transactionId) {
        return new AfterRow(storeId, productId, transactionId);
    }

    /** 先頭。 */
    record Start() implements SalesTransactionCursor {
        private static final Start INSTANCE = new Start();
    }

    /** 集計単位単位の位置。 */
    record AfterGroup(SalesGroupKey groupKey) implements SalesTransactionCursor {
        public AfterGroup {
            Objects.requireNonNull(groupKey, "groupKey は必須です");
        }
    }

    /** 行単位の位置。 */
    record AfterRow(String storeId, String productId, long transactionId) implements SalesTransactionCursor {
        public AfterRow {
            Objects.requireNonNull(storeId, "storeId は必須です");
            Objects.requireNonNull(productId, "productId は必須です");
        }
    }
}
