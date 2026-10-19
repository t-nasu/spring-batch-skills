package com.example.orderimport.domain.sales;

/**
 * 日次売上のリポジトリ。
 */
public interface DailySalesRepository {

    /**
     * 日次売上を登録する。同じ業務日付のものがあれば上書きする
     * （再実行しても結果が変わらないようにするため）。
     */
    void save(DailySales dailySales);
}
