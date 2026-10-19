package com.example.orderimport.domain.repository;

import com.example.orderimport.domain.model.order.CustomerCode;

/**
 * 顧客マスタのリポジトリ。
 *
 * <p>取り込み処理では存在チェックしか行わないため、顧客エンティティ全体ではなく
 * 存在有無だけを返す。
 */
public interface CustomerRepository {

    boolean exists(CustomerCode customerCode);
}
