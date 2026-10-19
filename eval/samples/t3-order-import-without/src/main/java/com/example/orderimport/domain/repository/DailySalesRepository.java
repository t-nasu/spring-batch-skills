package com.example.orderimport.domain.repository;

import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.sales.DailySales;
import java.util.List;

/**
 * 日次売上のリポジトリ。
 */
public interface DailySalesRepository {

    /**
     * 指定業務日付の日次売上を削除する。
     *
     * <p>集計ステップを再実行しても結果が二重にならないよう、登録前に呼び出す。
     *
     * @return 削除件数
     */
    int deleteByBusinessDate(BusinessDate businessDate);

    /**
     * 日次売上をまとめて登録する。
     *
     * @param dailySales 登録する日次売上。空リストの場合は何もしない
     */
    void saveAll(List<DailySales> dailySales);

    /** 指定業務日付の日次売上。主に検証・運用確認用。 */
    List<DailySales> findByBusinessDate(BusinessDate businessDate);
}
