package com.example.orderimport.domain.repository;

import com.example.orderimport.domain.model.BusinessDate;
import com.example.orderimport.domain.model.order.Order;
import com.example.orderimport.domain.model.sales.DailySales;
import java.util.List;

/**
 * 注文のリポジトリ。
 */
public interface OrderRepository {

    /**
     * 注文をまとめて登録する。
     *
     * <p>呼び出し元のトランザクションに参加する。同一注文番号が既に存在する場合は
     * 一意制約違反となり例外が送出される（黙って上書きしない）。
     *
     * @param orders 登録する注文。空リストの場合は何もしない
     */
    void saveAll(List<Order> orders);

    /**
     * 指定業務日付の注文を商品単位で集計する。
     *
     * <p>件数が最大 30 万件になりうるため、集計は SQL 側（GROUP BY）で行う。
     *
     * @return 商品コード昇順の集計結果。対象注文が 1 件もなければ空リスト
     */
    List<DailySales> aggregateDailySales(BusinessDate businessDate);
}
