package com.example.orderimport.domain.order;

import com.example.orderimport.domain.shared.BusinessDate;

import java.util.List;

/**
 * 注文のリポジトリ。
 */
public interface OrderRepository {

    /**
     * 注文をまとめて登録する。注文番号が同じ注文は上書きする
     * （再開したときに同じ行を二度読んでも結果が変わらないようにするため）。
     */
    void saveAll(List<Order> orders);

    /**
     * その業務日付に属する注文の件数と合計金額を求める。
     */
    OrderSalesSummary summarizeByOrderDate(BusinessDate orderDate);
}
