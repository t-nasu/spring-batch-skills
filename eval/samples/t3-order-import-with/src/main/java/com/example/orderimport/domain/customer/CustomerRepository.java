package com.example.orderimport.domain.customer;

/**
 * 顧客のリポジトリ。
 */
public interface CustomerRepository {

    /**
     * その顧客コードの顧客が存在するか。
     */
    boolean exists(CustomerCode customerCode);
}
