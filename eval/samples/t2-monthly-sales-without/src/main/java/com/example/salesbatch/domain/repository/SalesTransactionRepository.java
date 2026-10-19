package com.example.salesbatch.domain.repository;

import com.example.salesbatch.domain.model.RawSalesTransaction;
import com.example.salesbatch.domain.model.TargetMonth;

import java.util.List;

/**
 * 取引明細の参照ポート。実装はインフラ層（MyBatis）。
 */
public interface SalesTransactionRepository {

    /**
     * 対象年月の取引明細を、{@code (店舗 ID, 商品 ID, 取引 ID)} の昇順で 1 ページ分読み込む。
     *
     * @param targetMonth 対象年月（取引日がこの月に含まれる明細を抽出する）
     * @param cursor      読み込み開始位置
     * @param limit       取得件数の上限（1 以上）
     * @return 取引明細（取得できなければ空リスト）
     */
    List<RawSalesTransaction> findPage(TargetMonth targetMonth, SalesTransactionCursor cursor, int limit);
}
